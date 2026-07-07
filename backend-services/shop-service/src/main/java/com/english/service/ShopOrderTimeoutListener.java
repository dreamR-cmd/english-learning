package com.english.service;

import com.english.config.ShopRabbitConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ShopOrderTimeoutListener {
    private static final Logger log = LoggerFactory.getLogger(ShopOrderTimeoutListener.class);

    private final ShopService shopService;

    public ShopOrderTimeoutListener(ShopService shopService) {
        this.shopService = shopService;
    }

    @RabbitListener(queues = ShopRabbitConfig.ORDER_TIMEOUT_QUEUE)
    public void handleOrderTimeout(Long orderId) {
        log.info("RabbitMQ timeout message consumed: orderId={}, queue={}", orderId, ShopRabbitConfig.ORDER_TIMEOUT_QUEUE);
        /*
         * 订单超时消息到达这里时，不代表订单一定要取消：
         * 用户可能已经支付，或者订单已经被其他流程取消。
         * cancelExpiredOrder 内部做了状态判断，只取消 pending 订单，所以重复消费也是安全的。
         */
        shopService.cancelExpiredOrder(orderId);
    }
}
