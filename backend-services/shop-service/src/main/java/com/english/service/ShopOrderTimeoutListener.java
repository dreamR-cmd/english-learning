package com.english.service;

import com.english.config.ShopRocketMqConfig;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 订单超时取消消费者。
 * 消息由 RocketMQ 定时消息（延迟等级 14 = 10 分钟）投递，到达即表示下单已超过超时窗口。
 * 消息内容直接是 orderId 文本（由 RocketMqMessageCodec 约定）。
 */
@Component
@RocketMQMessageListener(
        topic = ShopRocketMqConfig.ORDER_TIMEOUT_TOPIC,
        consumerGroup = ShopRocketMqConfig.ORDER_TIMEOUT_CONSUMER_GROUP
)
public class ShopOrderTimeoutListener implements RocketMQListener<String> {
    private static final Logger log = LoggerFactory.getLogger(ShopOrderTimeoutListener.class);

    private final ShopService shopService;

    public ShopOrderTimeoutListener(ShopService shopService) {
        this.shopService = shopService;
    }

    @Override
    public void onMessage(String orderIdText) {
        Long orderId;
        try {
            orderId = Long.parseLong(orderIdText.trim());
        } catch (NumberFormatException error) {
            log.error("Invalid order timeout message payload: {}", orderIdText);
            return;
        }
        log.info("RocketMQ timeout message consumed: orderId={}", orderId);
        /*
         * 订单超时消息到达这里时，不代表订单一定要取消：
         * 用户可能已经支付，或者订单已经被其他流程取消。
         * cancelExpiredOrder 内部做了状态判断，只取消 pending 订单，所以重复消费也是安全的。
         */
        shopService.cancelExpiredOrder(orderId);
    }
}
