package com.english.service.impl;

import com.english.config.ShopRabbitConfig;
import com.english.dto.OrderTokenResponse;
import com.english.dto.SeckillOrderMessage;
import com.english.dto.SeckillOrderResultResponse;
import com.english.dto.SeckillOrderSubmitResponse;
import com.english.entity.ShopOrder;
import com.english.entity.ShopProduct;
import com.english.mapper.ShopOrderMapper;
import com.english.mapper.ShopProductMapper;
import com.english.service.ShopService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;

@Service
public class ShopServiceImpl implements ShopService {
    private static final Logger log = LoggerFactory.getLogger(ShopServiceImpl.class);
    private static final String STOCK_KEY_PREFIX = "shop:product:stock:";
    private static final String PRODUCT_NULL_KEY_PREFIX = "shop:product:null:";
    private static final String PRODUCT_BLOOM_ACTIVE_KEY = "shop:product:bloom:active";
    private static final String PRODUCT_BLOOM_KEY_A = "shop:product:bloom:a";
    private static final String PRODUCT_BLOOM_KEY_B = "shop:product:bloom:b";
    private static final String PRODUCT_BLOOM_REBUILD_LOCK_KEY = "lock:shop:product:bloom:rebuild";
    private static final String IDEMPOTENT_KEY_PREFIX = "shop:order:idempotent:";
    private static final String ORDER_TOKEN_KEY_PREFIX = "shop:order:token:";
    private static final String SECKILL_RESULT_KEY_PREFIX = "shop:seckill:result:";
    private static final String USED_TOKEN_PREFIX = "USED:";
    private static final String RESULT_PROCESSING = "processing";
    private static final String RESULT_SUCCESS_PREFIX = "success:";
    private static final String RESULT_FAIL_PREFIX = "fail:";
    private static final String ORDER_LOCK_KEY_PREFIX = "lock:shop:order:";
    private static final int REQUEST_ID_MAX_LENGTH = 64;
    private static final long ORDER_TOKEN_EXPIRE_SECONDS = 300;
    private static final long MAX_PRODUCT_ID = 100_000_000L;
    private static final long PRODUCT_BLOOM_BITMAP_SIZE = 1_000_000L;
    private static final int PRODUCT_BLOOM_HASH_COUNT = 5;
    private static final long PRODUCT_NULL_CACHE_BASE_SECONDS = 180;
    private static final long PRODUCT_NULL_CACHE_RANDOM_SECONDS = 120;

    private final ShopProductMapper productMapper;
    private final ShopOrderMapper orderMapper;
    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final TransactionTemplate transactionTemplate;
    private final RedissonClient redissonClient;
    private final long orderTimeoutMinutes;

    public ShopServiceImpl(ShopProductMapper productMapper,
                           ShopOrderMapper orderMapper,
                           StringRedisTemplate redisTemplate,
                           RabbitTemplate rabbitTemplate,
                           TransactionTemplate transactionTemplate,
                           RedissonClient redissonClient,
                           @Value("${shop.order.timeout-minutes:30}") long orderTimeoutMinutes) {
        this.productMapper = productMapper;
        this.orderMapper = orderMapper;
        this.redisTemplate = redisTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.transactionTemplate = transactionTemplate;
        this.redissonClient = redissonClient;
        this.orderTimeoutMinutes = orderTimeoutMinutes;
    }

    @Override
    public List<ShopProduct> getProducts() {
        List<ShopProduct> products = productMapper.findByActiveTrueOrderBySortOrderAscIdAsc();
        products.forEach(product -> addProductToBloom(product.getId()));
        products.forEach(this::syncStockFromRedis);
        return products;
    }

    @PostConstruct
    public void initProductBloomFilter() {
        rebuildProductBloomFilter();
    }

    @Scheduled(fixedDelayString = "${shop.product.bloom.rebuild-delay-ms:600000}")
    public void scheduledRebuildProductBloomFilter() {
        rebuildProductBloomFilter();
    }

    @Override
    public OrderTokenResponse createOrderToken(Long userId, Long productId) {
        if (userId == null || productId == null) {
            throw new RuntimeException("用户和商品不能为空");
        }
        findActiveProductOrThrow(productId);

        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(orderTokenKey(userId, token), String.valueOf(productId),
                ORDER_TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS);
        return new OrderTokenResponse(token, ORDER_TOKEN_EXPIRE_SECONDS);
    }

    @Override
    public ShopOrder createOrder(Long userId, Long productId, String requestId) {
        String normalizedRequestId = normalizeRequestId(requestId);
        if (userId == null || productId == null) {
            throw new RuntimeException("用户和商品不能为空");
        }

        ShopOrder existing = findExistingOrder(userId, normalizedRequestId);
        if (existing != null) {
            return existing;
        }

        validateOrderToken(userId, productId, normalizedRequestId);

        RLock orderLock = redissonClient.getLock(orderLockKey(userId, normalizedRequestId));
        boolean orderLocked = false;
        try {
            orderLocked = orderLock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!orderLocked) {
                throw new RuntimeException("订单正在创建中，请勿重复提交");
            }

            existing = findExistingOrder(userId, normalizedRequestId);
            if (existing != null) {
                return existing;
            }

            return createOrderAfterLock(userId, productId, normalizedRequestId);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("订单创建被中断，请稍后重试");
        } finally {
            if (orderLocked && orderLock.isHeldByCurrentThread()) {
                orderLock.unlock();
            }
        }
    }

    @Override
    public SeckillOrderSubmitResponse submitSeckillOrder(Long userId, Long productId, String requestId) {
        String normalizedRequestId = normalizeRequestId(requestId);
        if (userId == null || productId == null) {
            throw new RuntimeException("用户和商品不能为空");
        }

        ShopOrder existing = findExistingOrder(userId, normalizedRequestId);
        if (existing != null) {
            return new SeckillOrderSubmitResponse("success", normalizedRequestId, existing.getId(), "订单已创建");
        }

        validateOrderToken(userId, productId, normalizedRequestId);
        ShopProduct product = findActiveProductOrThrow(productId);

        String resultKey = seckillResultKey(userId, normalizedRequestId);
        String result = redisTemplate.opsForValue().get(resultKey);
        if (result != null) {
            return buildSubmitResponseFromResult(normalizedRequestId, result);
        }

        boolean firstSubmit = Boolean.TRUE.equals(redisTemplate.opsForValue()
                .setIfAbsent(resultKey, RESULT_PROCESSING, ORDER_TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS));
        if (!firstSubmit) {
            return new SeckillOrderSubmitResponse("queued", normalizedRequestId, null, "订单排队中");
        }

        boolean redisReserved = false;
        try {
            reserveRedisStock(product);
            redisReserved = true;
            rabbitTemplate.convertAndSend(
                    ShopRabbitConfig.ORDER_EXCHANGE,
                    ShopRabbitConfig.SECKILL_ORDER_ROUTING_KEY,
                    new SeckillOrderMessage(userId, productId, normalizedRequestId)
            );
            log.info("Seckill order queued: userId={}, productId={}, requestId={}", userId, productId, normalizedRequestId);
            return new SeckillOrderSubmitResponse("queued", normalizedRequestId, null, "订单已进入排队");
        } catch (RuntimeException error) {
            if (redisReserved) {
                restoreRedisStock(productId);
            }
            redisTemplate.opsForValue().set(resultKey, RESULT_FAIL_PREFIX + "系统繁忙，请稍后重试", ORDER_TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS);
            throw new RuntimeException("秒杀请求提交失败，请稍后重试");
        }
    }

    @Override
    public void consumeSeckillOrder(Long userId, Long productId, String requestId) {
        String normalizedRequestId = normalizeRequestId(requestId);
        String resultKey = seckillResultKey(userId, normalizedRequestId);
        ShopOrder existing = findExistingOrder(userId, normalizedRequestId);
        if (existing != null) {
            redisTemplate.opsForValue().set(resultKey, RESULT_SUCCESS_PREFIX + existing.getId(), 1, TimeUnit.DAYS);
            return;
        }

        try {
            validateOrderToken(userId, productId, normalizedRequestId);
            ShopOrder saved = createQueuedOrder(userId, productId, normalizedRequestId);
            redisTemplate.opsForValue().set(resultKey, RESULT_SUCCESS_PREFIX + saved.getId(), 1, TimeUnit.DAYS);
            redisTemplate.opsForValue().set(orderTokenKey(userId, normalizedRequestId), USED_TOKEN_PREFIX + saved.getId(), 1, TimeUnit.DAYS);
            trySendOrderTimeoutMessage(saved.getId());
            log.info("Seckill order consumed: orderId={}, userId={}, productId={}, requestId={}",
                    saved.getId(), userId, productId, normalizedRequestId);
        } catch (RuntimeException error) {
            restoreRedisStock(productId);
            redisTemplate.opsForValue().set(resultKey, RESULT_FAIL_PREFIX + error.getMessage(), ORDER_TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS);
            log.warn("Seckill order failed: userId={}, productId={}, requestId={}, error={}",
                    userId, productId, normalizedRequestId, error.getMessage());
        }
    }

    @Override
    public SeckillOrderResultResponse getSeckillOrderResult(Long userId, String requestId) {
        String normalizedRequestId = normalizeRequestId(requestId);
        ShopOrder existing = findExistingOrder(userId, normalizedRequestId);
        if (existing != null) {
            return new SeckillOrderResultResponse("success", "订单已创建", existing);
        }

        String result = redisTemplate.opsForValue().get(seckillResultKey(userId, normalizedRequestId));
        if (result == null || RESULT_PROCESSING.equals(result)) {
            return new SeckillOrderResultResponse("queued", "订单排队中", null);
        }
        if (result.startsWith(RESULT_SUCCESS_PREFIX)) {
            Long orderId = Long.valueOf(result.substring(RESULT_SUCCESS_PREFIX.length()));
            ShopOrder order = orderMapper.findById(orderId).orElse(null);
            return new SeckillOrderResultResponse(order == null ? "queued" : "success",
                    order == null ? "订单排队中" : "订单已创建", order);
        }
        if (result.startsWith(RESULT_FAIL_PREFIX)) {
            return new SeckillOrderResultResponse("failed", result.substring(RESULT_FAIL_PREFIX.length()), null);
        }
        return new SeckillOrderResultResponse("queued", "订单排队中", null);
    }

    private ShopOrder createOrderAfterLock(Long userId, Long productId, String normalizedRequestId) {
        String idempotentKey = idempotentKey(userId, normalizedRequestId);
        boolean locked = Boolean.TRUE.equals(redisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, "processing", 60, TimeUnit.SECONDS));
        if (!locked) {
            ShopOrder createdByOtherRequest = waitForExistingOrder(userId, normalizedRequestId);
            if (createdByOtherRequest != null) {
                return createdByOtherRequest;
            }
            throw new RuntimeException("订单正在创建中，请勿重复提交");
        }

        try {
            return createOrderWithLock(userId, productId, normalizedRequestId, idempotentKey);
        } catch (RuntimeException error) {
            redisTemplate.delete(idempotentKey);
            throw error;
        }
    }

    private ShopOrder createOrderWithLock(Long userId, Long productId, String requestId, String idempotentKey) {
        ShopProduct product = findActiveProductOrThrow(productId);

        AtomicBoolean stockReserved = new AtomicBoolean(false);
        try {
            ShopOrder saved = transactionTemplate.execute(status -> {
                reserveStock(product);
                stockReserved.set(true);

                ShopOrder order = new ShopOrder();
                order.setOrderNo(buildOrderNo());
                order.setRequestId(requestId);
                order.setUserId(userId);
                order.setProductId(product.getId());
                order.setProductName(product.getTitle());
                order.setIcon(product.getIcon());
                order.setAmount(product.getPrice());
                order.setStatus(ShopOrder.STATUS_PENDING);
                order.setExpireAt(LocalDateTime.now().plusMinutes(orderTimeoutMinutes));

                return orderMapper.save(order);
            });
            redisTemplate.opsForValue().set(idempotentKey, String.valueOf(saved.getId()), 1, TimeUnit.DAYS);
            redisTemplate.opsForValue().set(orderTokenKey(userId, requestId), USED_TOKEN_PREFIX + saved.getId(), 1, TimeUnit.DAYS);
            log.info("Shop order created: orderId={}, orderNo={}, productId={}, userId={}, requestId={}, timeoutMinutes={}",
                    saved.getId(), saved.getOrderNo(), saved.getProductId(), saved.getUserId(), saved.getRequestId(), orderTimeoutMinutes);
            trySendOrderTimeoutMessage(saved.getId());
            return saved;
        } catch (DataIntegrityViolationException error) {
            if (stockReserved.get()) {
                restoreRedisStock(productId);
            }
            ShopOrder existing = waitForExistingOrder(userId, requestId);
            if (existing != null) {
                return existing;
            }
            throw error;
        } catch (RuntimeException error) {
            if (stockReserved.get()) {
                restoreRedisStock(productId);
            }
            throw error;
        }
    }

    private ShopOrder createQueuedOrder(Long userId, Long productId, String requestId) {
        ShopProduct product = findActiveProductOrThrow(productId);

        return transactionTemplate.execute(status -> {
            int updated = productMapper.decreaseStock(product.getId());
            if (updated == 0) {
                throw new RuntimeException("商品库存不足");
            }

            ShopOrder order = new ShopOrder();
            order.setOrderNo(buildOrderNo());
            order.setRequestId(requestId);
            order.setUserId(userId);
            order.setProductId(product.getId());
            order.setProductName(product.getTitle());
            order.setIcon(product.getIcon());
            order.setAmount(product.getPrice());
            order.setStatus(ShopOrder.STATUS_PENDING);
            order.setExpireAt(LocalDateTime.now().plusMinutes(orderTimeoutMinutes));
            return orderMapper.save(order);
        });
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
        log.info("Shop order paid: orderId={}, orderNo={}, userId={}", order.getId(), order.getOrderNo(), order.getUserId());
        return orderMapper.save(order);
    }

    @Override
    @Transactional
    public void cancelExpiredOrder(Long orderId) {
        ShopOrder order = orderMapper.findById(orderId).orElse(null);
        if (order == null || !ShopOrder.STATUS_PENDING.equals(order.getStatus())) {
            return;
        }

        order.setStatus(ShopOrder.STATUS_CANCELED);
        order.setCanceledAt(LocalDateTime.now());
        orderMapper.save(order);
        restoreStock(order.getProductId());
        log.info("Shop order canceled by timeout: orderId={}, orderNo={}, productId={}",
                order.getId(), order.getOrderNo(), order.getProductId());
    }

    private String normalizeRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            throw new RuntimeException("requestId不能为空");
        }
        String normalized = requestId.trim();
        if (normalized.length() > REQUEST_ID_MAX_LENGTH) {
            throw new RuntimeException("requestId长度不能超过64");
        }
        return normalized;
    }

    private ShopOrder findExistingOrder(Long userId, String requestId) {
        return orderMapper.findByUserIdAndRequestId(userId, requestId).orElse(null);
    }

    private void validateOrderToken(Long userId, Long productId, String requestId) {
        String tokenValue = redisTemplate.opsForValue().get(orderTokenKey(userId, requestId));
        if (tokenValue == null || tokenValue.isBlank()) {
            throw new RuntimeException("幂等性token无效或已过期");
        }
        if (tokenValue.startsWith(USED_TOKEN_PREFIX)) {
            throw new RuntimeException("幂等性token已使用，请查询订单");
        }
        if (!String.valueOf(productId).equals(tokenValue)) {
            throw new RuntimeException("幂等性token与商品不匹配");
        }
    }

    private ShopOrder waitForExistingOrder(Long userId, String requestId) {
        for (int i = 0; i < 120; i++) {
            ShopOrder existing = findExistingOrder(userId, requestId);
            if (existing != null) {
                return existing;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }

    private void reserveStock(ShopProduct product) {
        reserveRedisStock(product);

        int updated = productMapper.decreaseStock(product.getId());
        if (updated == 0) {
            redisTemplate.opsForValue().increment(stockKey(product.getId()));
            throw new RuntimeException("商品库存不足");
        }
    }

    private void reserveRedisStock(ShopProduct product) {
        String stockKey = stockKey(product.getId());
        redisTemplate.opsForValue().setIfAbsent(stockKey, String.valueOf(product.getStock()), 1, TimeUnit.DAYS);

        Long stock = redisTemplate.opsForValue().decrement(stockKey);
        log.info("Redis stock reserved: productId={}, stockKey={}, remainingStock={}",
                product.getId(), stockKey, stock);
        if (stock == null || stock < 0) {
            redisTemplate.opsForValue().increment(stockKey);
            throw new RuntimeException("商品库存不足");
        }
    }

    private void restoreStock(Long productId) {
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
            redisTemplate.opsForValue().set(stockKey, String.valueOf(product.getStock()), 1, TimeUnit.DAYS);
            return;
        }

        try {
            product.setStock(Integer.valueOf(cached));
        } catch (NumberFormatException ignored) {
            redisTemplate.opsForValue().set(stockKey, String.valueOf(product.getStock()), 1, TimeUnit.DAYS);
        }
    }

    private ShopProduct findActiveProductOrThrow(Long productId) {
        validateProductId(productId);
        if (!mightProductExist(productId)) {
            throw new RuntimeException("商品不存在或已下架");
        }
        String nullKey = productNullKey(productId);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(nullKey))) {
            throw new RuntimeException("商品不存在或已下架");
        }

        ShopProduct product = productMapper.findById(productId)
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .orElse(null);
        if (product == null) {
            cacheNullProduct(productId);
            throw new RuntimeException("商品不存在或已下架");
        }

        redisTemplate.delete(nullKey);
        addProductToBloom(productId);
        return product;
    }

    private void validateProductId(Long productId) {
        if (productId == null || productId <= 0 || productId > MAX_PRODUCT_ID) {
            throw new RuntimeException("商品不存在或已下架");
        }
    }

    private boolean mightProductExist(Long productId) {
        String bloomKey = activeProductBloomKey();
        if (bloomKey == null) {
            return true;
        }
        for (int seed = 0; seed < PRODUCT_BLOOM_HASH_COUNT; seed++) {
            if (!Boolean.TRUE.equals(redisTemplate.opsForValue().getBit(bloomKey, bloomOffset(productId, seed)))) {
                return false;
            }
        }
        return true;
    }

    private void cacheNullProduct(Long productId) {
        long ttl = PRODUCT_NULL_CACHE_BASE_SECONDS
                + ThreadLocalRandom.current().nextLong(PRODUCT_NULL_CACHE_RANDOM_SECONDS + 1);
        redisTemplate.opsForValue().set(productNullKey(productId), "1", ttl, TimeUnit.SECONDS);
    }

    private void addProductToBloom(Long productId) {
        if (productId == null || productId <= 0 || productId > MAX_PRODUCT_ID) {
            return;
        }
        String bloomKey = activeProductBloomKey();
        if (bloomKey == null) {
            bloomKey = PRODUCT_BLOOM_KEY_A;
            redisTemplate.opsForValue().set(PRODUCT_BLOOM_ACTIVE_KEY, bloomKey);
        }
        addProductToBloom(bloomKey, productId);
    }

    private void addProductToBloom(String bloomKey, Long productId) {
        for (int seed = 0; seed < PRODUCT_BLOOM_HASH_COUNT; seed++) {
            redisTemplate.opsForValue().setBit(bloomKey, bloomOffset(productId, seed), true);
        }
    }

    private void rebuildProductBloomFilter() {
        boolean locked = Boolean.TRUE.equals(redisTemplate.opsForValue()
                .setIfAbsent(PRODUCT_BLOOM_REBUILD_LOCK_KEY, "1", 5, TimeUnit.MINUTES));
        if (!locked) {
            return;
        }
        try {
            String currentKey = activeProductBloomKey();
            String nextKey = PRODUCT_BLOOM_KEY_A.equals(currentKey) ? PRODUCT_BLOOM_KEY_B : PRODUCT_BLOOM_KEY_A;
            redisTemplate.delete(nextKey);
            productMapper.findByActiveTrueOrderBySortOrderAscIdAsc()
                    .forEach(product -> addProductToBloom(nextKey, product.getId()));
            redisTemplate.opsForValue().set(PRODUCT_BLOOM_ACTIVE_KEY, nextKey);
            if (currentKey != null && !currentKey.equals(nextKey)) {
                redisTemplate.expire(currentKey, 1, TimeUnit.DAYS);
            }
            log.info("Product bloom filter rebuilt: activeKey={}", nextKey);
        } catch (RuntimeException error) {
            log.warn("Failed to rebuild product bloom filter", error);
        } finally {
            redisTemplate.delete(PRODUCT_BLOOM_REBUILD_LOCK_KEY);
        }
    }

    private String activeProductBloomKey() {
        String key = redisTemplate.opsForValue().get(PRODUCT_BLOOM_ACTIVE_KEY);
        if (PRODUCT_BLOOM_KEY_A.equals(key) || PRODUCT_BLOOM_KEY_B.equals(key)) {
            return key;
        }
        return null;
    }

    private long bloomOffset(Long productId, int seed) {
        long hash = productId ^ (0x9E3779B97F4A7C15L * (seed + 1));
        hash ^= (hash >>> 33);
        hash *= 0xff51afd7ed558ccdL;
        hash ^= (hash >>> 33);
        hash *= 0xc4ceb9fe1a85ec53L;
        hash ^= (hash >>> 33);
        return Math.floorMod(hash, PRODUCT_BLOOM_BITMAP_SIZE);
    }

    private void sendOrderTimeoutMessage(Long orderId) {
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
        log.info("RabbitMQ timeout message produced: orderId={}, exchange={}, routingKey={}, ttlMillis={}",
                orderId, ShopRabbitConfig.ORDER_EXCHANGE, ShopRabbitConfig.ORDER_DELAY_ROUTING_KEY, ttl);
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

    private String productNullKey(Long productId) {
        return PRODUCT_NULL_KEY_PREFIX + productId;
    }

    private String idempotentKey(Long userId, String requestId) {
        return IDEMPOTENT_KEY_PREFIX + userId + ":" + requestId;
    }

    private String orderTokenKey(Long userId, String requestId) {
        return ORDER_TOKEN_KEY_PREFIX + userId + ":" + requestId;
    }

    private String orderLockKey(Long userId, String requestId) {
        return ORDER_LOCK_KEY_PREFIX + userId + ":" + requestId;
    }

    private String seckillResultKey(Long userId, String requestId) {
        return SECKILL_RESULT_KEY_PREFIX + userId + ":" + requestId;
    }

    private SeckillOrderSubmitResponse buildSubmitResponseFromResult(String requestId, String result) {
        if (result.startsWith(RESULT_SUCCESS_PREFIX)) {
            return new SeckillOrderSubmitResponse("success", requestId,
                    Long.valueOf(result.substring(RESULT_SUCCESS_PREFIX.length())), "订单已创建");
        }
        if (result.startsWith(RESULT_FAIL_PREFIX)) {
            return new SeckillOrderSubmitResponse("failed", requestId, null,
                    result.substring(RESULT_FAIL_PREFIX.length()));
        }
        return new SeckillOrderSubmitResponse("queued", requestId, null, "订单排队中");
    }

    private String buildOrderNo() {
        String prefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "EL" + prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
