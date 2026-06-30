package com.english.service.impl;

import com.english.config.ShopRabbitConfig;
import com.english.entity.ShopOrder;
import com.english.entity.ShopProduct;
import com.english.mapper.ShopOrderMapper;
import com.english.mapper.ShopProductMapper;
import com.english.service.ShopService;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class ShopServiceImpl implements ShopService {
    private static final Logger log = LoggerFactory.getLogger(ShopServiceImpl.class);
    // Redis 中商品库存 key 的前缀，最终 key 形如：shop:product:stock:1
    private static final String STOCK_KEY_PREFIX = "shop:product:stock:";

    private final ShopProductMapper productMapper;
    private final ShopOrderMapper orderMapper;
    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final long orderTimeoutMinutes;

    public ShopServiceImpl(ShopProductMapper productMapper,
                           ShopOrderMapper orderMapper,
                           StringRedisTemplate redisTemplate,
                           RabbitTemplate rabbitTemplate,
                           @Value("${shop.order.timeout-minutes:30}") long orderTimeoutMinutes) {
        this.productMapper = productMapper;
        this.orderMapper = orderMapper;
        this.redisTemplate = redisTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.orderTimeoutMinutes = orderTimeoutMinutes;
    }

    @Override
    public List<ShopProduct> getProducts() {
        List<ShopProduct> products = productMapper.findByActiveTrueOrderBySortOrderAscIdAsc();
        /*
         * 商品库存展示优先看 Redis。
         *
         * 原因：高并发下库存扣减先发生在 Redis，Redis 的值比数据库更接近实时可售库存。
         * 如果 Redis 里还没有这个商品的库存，则用数据库库存初始化缓存。
         */
        products.forEach(this::syncStockFromRedis);
        return products;
    }

    @Override
    @Transactional
    public ShopOrder createOrder(Long userId, Long productId) {
        /*
         * 下单流程：
         *
         * 1. 校验用户和商品。
         * 2. reserveStock：先扣 Redis，再扣数据库，防止高并发超卖。
         * 3. 创建待支付订单，设置 30 分钟过期时间。
         * 4. 尝试发送 RabbitMQ 延迟消息，到期后自动检查并取消未支付订单。
         *
         * 注意：这里把“占库存”和“创建订单”放在一个事务里处理数据库状态。
         * Redis 不参与数据库事务，所以创建订单失败时需要手动 restoreStock 回补 Redis 和数据库库存。
         */
        if (userId == null || productId == null) {
            throw new RuntimeException("用户和商品不能为空");
        }

        ShopProduct product = productMapper.findById(productId)
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .orElseThrow(() -> new RuntimeException("商品不存在或已下架"));

        reserveStock(product);

        try {
            ShopOrder order = new ShopOrder();
            order.setOrderNo(buildOrderNo());
            order.setUserId(userId);
            order.setProductId(product.getId());
            order.setProductName(product.getTitle());
            order.setIcon(product.getIcon());
            order.setAmount(product.getPrice());
            order.setStatus(ShopOrder.STATUS_PENDING);
            // 业务过期时间落库，支付时会再次校验，避免只依赖 MQ 消息。
            order.setExpireAt(LocalDateTime.now().plusMinutes(orderTimeoutMinutes));
            ShopOrder saved = orderMapper.save(order);
            trySendOrderTimeoutMessage(saved.getId());
            return saved;
        } catch (RuntimeException error) {
            /*
             * 如果订单保存失败，当前 Hibernate Session 可能已经处于异常状态，
             * 不能再在同一个事务里执行 JPA 查询或更新，否则会触发
             * "don't flush the Session after an exception occurs"。
             *
             * 数据库扣库存和订单保存处于同一个事务，异常会整体回滚；这里只需要回补
             * 不参与数据库事务的 Redis 库存。
             */
            restoreRedisStock(productId);
            throw error;
        }
    }

    @Override
    public List<ShopOrder> getOrders(Long userId, String status) {
        if (userId == null) {
            throw new RuntimeException("用户不能为空");
        }
        if (status == null || status.isBlank() || "all".equals(status)) {
            return orderMapper.findByUserIdOrderByCreatedAtDesc(userId);
        }
        return orderMapper.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status);
    }

    @Override
    @Transactional
    public ShopOrder payOrder(Long userId, Long orderId) {
        /*
         * 支付接口只允许支付当前用户自己的 pending 订单。
         * 即使 RabbitMQ 延迟消息还没来，只要订单已过 expireAt，也会主动取消。
         * 这样可以避免“超时订单刚好被用户支付”的边界问题。
         */
        ShopOrder order = orderMapper.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        if (ShopOrder.STATUS_PAID.equals(order.getStatus())) {
            return order;
        }
        if (!ShopOrder.STATUS_PENDING.equals(order.getStatus())) {
            throw new RuntimeException("订单状态不可支付");
        }
        if (order.getExpireAt() != null && order.getExpireAt().isBefore(LocalDateTime.now())) {
            cancelExpiredOrder(order.getId());
            throw new RuntimeException("订单已超时取消");
        }

        order.setStatus(ShopOrder.STATUS_PAID);
        order.setPaidAt(LocalDateTime.now());
        return orderMapper.save(order);
    }

    @Override
    @Transactional
    public void cancelExpiredOrder(Long orderId) {
        /*
         * MQ 消费者和支付接口都可能调用这个方法，所以它必须是幂等的：
         * - 订单不存在：直接返回。
         * - 订单不是 pending：说明已支付或已取消，直接返回。
         * - 只有 pending 订单才会被取消并回补库存。
         */
        ShopOrder order = orderMapper.findById(orderId).orElse(null);
        if (order == null || !ShopOrder.STATUS_PENDING.equals(order.getStatus())) {
            return;
        }

        order.setStatus(ShopOrder.STATUS_CANCELED);
        order.setCanceledAt(LocalDateTime.now());
        orderMapper.save(order);
        restoreStock(order.getProductId());
    }

    private void reserveStock(ShopProduct product) {
        String stockKey = stockKey(product.getId());
        /*
         * setIfAbsent 用数据库库存初始化 Redis 库存缓存。
         * 设置 1 天过期时间是为了避免缓存永久脏数据；真实项目可根据业务改成更长或主动刷新。
         */
        redisTemplate.opsForValue().setIfAbsent(stockKey, String.valueOf(product.getStock()), 1, TimeUnit.DAYS);

        /*
         * Redis decrement 是单线程原子操作。
         * 多个用户同时下单时，只有库存数以内的请求能扣到 >= 0 的结果。
         * 如果扣成负数，说明库存已经不够，需要立刻 increment 回补。
         */
        Long stock = redisTemplate.opsForValue().decrement(stockKey);
        if (stock == null || stock < 0) {
            redisTemplate.opsForValue().increment(stockKey);
            throw new RuntimeException("商品库存不足");
        }

        /*
         * 数据库也必须做原子扣减，不能先查库存再 save。
         * decreaseStock 的 SQL 条件包含 stock > 0：
         * update shop_products set stock = stock - 1 where id = ? and stock > 0
         * 返回 0 表示数据库层面库存不足，需要把 Redis 刚才扣掉的库存回补。
         */
        int updated = productMapper.decreaseStock(product.getId());
        if (updated == 0) {
            redisTemplate.opsForValue().increment(stockKey);
            throw new RuntimeException("商品库存不足");
        }
    }

    private void restoreStock(Long productId) {
        /*
         * 回补库存场景：
         * - 创建订单失败。
         * - 订单超时未支付被取消。
         *
         * 这里同时回补数据库和 Redis，保证后续商品列表看到的库存与数据库最终一致。
         */
        productMapper.findById(productId).ifPresent(product -> {
            productMapper.increaseStock(productId);
            redisTemplate.opsForValue().increment(stockKey(productId));
        });
    }

    private void restoreRedisStock(Long productId) {
        redisTemplate.opsForValue().increment(stockKey(productId));
    }

    private void syncStockFromRedis(ShopProduct product) {
        String stockKey = stockKey(product.getId());
        String cached = redisTemplate.opsForValue().get(stockKey);
        if (cached == null) {
            // 缓存未命中时，把数据库库存写入 Redis，之后商品页读到的就是缓存库存。
            redisTemplate.opsForValue().set(stockKey, String.valueOf(product.getStock()), 1, TimeUnit.DAYS);
            return;
        }

        try {
            // 用 Redis 库存覆盖返回给前端的 stock 字段，让商品页展示更实时的可售库存。
            product.setStock(Integer.valueOf(cached));
        } catch (NumberFormatException ignored) {
            // 如果缓存被误写成非数字，丢弃脏值并用数据库库存重建缓存。
            redisTemplate.opsForValue().set(stockKey, String.valueOf(product.getStock()), 1, TimeUnit.DAYS);
        }
    }

    private void sendOrderTimeoutMessage(Long orderId) {
        /*
         * 给单条消息设置 TTL。
         * 消息在延迟队列中过期后，会通过死信配置进入超时队列。
         */
        int ttl = Math.toIntExact(TimeUnit.MINUTES.toMillis(orderTimeoutMinutes));
        MessagePostProcessor processor = message -> {
            message.getMessageProperties().setExpiration(String.valueOf(ttl));
            return message;
        };
        rabbitTemplate.convertAndSend(
                ShopRabbitConfig.ORDER_EXCHANGE,
                ShopRabbitConfig.ORDER_DELAY_ROUTING_KEY,
                orderId,
                processor
        );
    }

    private void trySendOrderTimeoutMessage(Long orderId) {
        try {
            sendOrderTimeoutMessage(orderId);
        } catch (AmqpException error) {
            log.warn("Failed to send timeout message for shop order {}", orderId, error);
        }
    }

    private String stockKey(Long productId) {
        return STOCK_KEY_PREFIX + productId;
    }

    private String buildOrderNo() {
        // 订单号由时间戳 + UUID 片段组成，便于排查且基本避免并发重复。
        String prefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "EL" + prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
