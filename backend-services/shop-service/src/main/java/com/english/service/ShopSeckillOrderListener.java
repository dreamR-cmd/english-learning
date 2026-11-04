package com.english.service;

import com.english.config.ShopRocketMqConfig;
import com.english.dto.SeckillOrderMessage;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 秒杀订单异步落库消费者。
 * 消费抛出异常时 RocketMQ 自动重试（默认 16 次后退避间隔递增），
 * 重试耗尽后消息进入系统死信 Topic %DLQ%+{consumerGroup}，等价原 RabbitMQ DLQ。
 */
@Component
@RocketMQMessageListener(
        topic = ShopRocketMqConfig.SECKILL_ORDER_TOPIC,
        consumerGroup = ShopRocketMqConfig.SECKILL_ORDER_CONSUMER_GROUP,
        selectorExpression = ShopRocketMqConfig.SECKILL_ORDER_TAG
)
public class ShopSeckillOrderListener implements RocketMQListener<SeckillOrderMessage> {
    private static final Logger log = LoggerFactory.getLogger(ShopSeckillOrderListener.class);

    private final ShopService shopService;

    public ShopSeckillOrderListener(ShopService shopService) {
        this.shopService = shopService;
    }

    @Override
    public void onMessage(SeckillOrderMessage message) {
        log.info("Seckill order message consumed: userId={}, productId={}, requestId={}",
                message.getUserId(), message.getProductId(), message.getRequestId());
        shopService.consumeSeckillOrder(message.getUserId(), message.getProductId(), message.getRequestId());
    }
}
