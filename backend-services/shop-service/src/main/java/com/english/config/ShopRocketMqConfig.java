package com.english.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * RocketMQ 常量与消息序列化工具（替代原 ShopRabbitConfig）。
 *
 * 延迟/超时说明：
 * - 订单超时使用 RocketMQ 定时消息（messageDelayLevel=14，即 10 分钟），
 *   代替原 RabbitMQ “普通队列 TTL + 死信队列”方案，不再需要延迟队列和死信交换机。
 * - 秒杀订单消费失败时由 RocketMQ 消费重试机制（默认 16 次）自动投递到系统死信 Topic
 *   （%DLQ%+consumerGroup），不再手动声明 DLQ。
 */
public final class ShopRocketMqConfig {
    private ShopRocketMqConfig() {
    }

    public static final String SECKILL_ORDER_TOPIC = "english-shop-seckill-order-topic";
    public static final String SECKILL_ORDER_TAG = "seckill-order";
    public static final String ORDER_TIMEOUT_TOPIC = "english-shop-order-timeout-topic";
    public static final String ORDER_TIMEOUT_TAG = "order-timeout";

    /** 消费组：秒杀订单异步落库 */
    public static final String SECKILL_ORDER_CONSUMER_GROUP = "english-shop-seckill-order-consumer";
    /** 消费组：订单超时取消 */
    public static final String ORDER_TIMEOUT_CONSUMER_GROUP = "english-shop-order-timeout-consumer";
}

