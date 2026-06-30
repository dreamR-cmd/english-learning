package com.english.service;

import com.english.config.ShopRabbitConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ShopOrderTimeoutListener {
    private final ShopService shopService;

    public ShopOrderTimeoutListener(ShopService shopService) {
        this.shopService = shopService;
    }

    @RabbitListener(queues = ShopRabbitConfig.ORDER_TIMEOUT_QUEUE)
    public void handleOrderTimeout(Long orderId) {
        /*
         * 订单超时消息到达这里时，不代表订单一定要取消：
         * 用户可能已经支付，或者订单已经被其他流程取消。
         * cancelExpiredOrder 内部做了状态判断，只取消 pending 订单，所以重复消费也是安全的。
         */
        shopService.cancelExpiredOrder(orderId);
    }
}
