package com.english.service;

import com.english.config.ShopRabbitConfig;
import com.english.dto.SeckillOrderMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ShopSeckillOrderListener {
    private static final Logger log = LoggerFactory.getLogger(ShopSeckillOrderListener.class);

    private final ShopService shopService;

    public ShopSeckillOrderListener(ShopService shopService) {
        this.shopService = shopService;
    }

    @RabbitListener(queues = ShopRabbitConfig.SECKILL_ORDER_QUEUE)
    public void handleSeckillOrder(SeckillOrderMessage message) {
        log.info("Seckill order message consumed: userId={}, productId={}, requestId={}",
                message.getUserId(), message.getProductId(), message.getRequestId());
        shopService.consumeSeckillOrder(message.getUserId(), message.getProductId(), message.getRequestId());
    }
}
