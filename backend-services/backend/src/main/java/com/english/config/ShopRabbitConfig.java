package com.english.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ShopRabbitConfig {
    /*
     * RabbitMQ 这里用“普通队列 TTL + 死信队列”的方式实现延迟任务：
     *
     * 1. 创建订单时，把 orderId 发送到 ORDER_DELAY_QUEUE。
     * 2. 这条消息在延迟队列里不会被业务消费者消费，只是等待过期。
     * 3. 消息 TTL 到期后，RabbitMQ 自动把它投递到配置的死信交换机和死信路由。
     * 4. 最终消息进入 ORDER_TIMEOUT_QUEUE，由 ShopOrderTimeoutListener 消费并取消未支付订单。
     *
     * 好处：不依赖 rabbitmq_delayed_message_exchange 插件，普通 RabbitMQ 就能跑。
     * 注意：这种方案适合订单超时这类“分钟级”延迟，不适合需要毫秒级严格定时的场景。
     */
    public static final String ORDER_EXCHANGE = "english.shop.order.exchange";
    public static final String ORDER_DELAY_QUEUE = "english.shop.order.delay.queue";
    public static final String ORDER_TIMEOUT_QUEUE = "english.shop.order.timeout.queue";
    public static final String ORDER_DELAY_ROUTING_KEY = "order.delay";
    public static final String ORDER_TIMEOUT_ROUTING_KEY = "order.timeout";

    @Bean
    public DirectExchange orderExchange() {
        // durable=true 表示 RabbitMQ 重启后交换机仍然存在；autoDelete=false 表示不自动删除。
        return new DirectExchange(ORDER_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderDelayQueue() {
        /*
         * 延迟队列本身没有消费者。
         * deadLetterExchange/deadLetterRoutingKey 决定消息过期后投递到哪里。
         * 每条消息的具体过期时间在发送时通过 message.properties.expiration 设置。
         */
        return QueueBuilder.durable(ORDER_DELAY_QUEUE)
                .deadLetterExchange(ORDER_EXCHANGE)
                .deadLetterRoutingKey(ORDER_TIMEOUT_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue orderTimeoutQueue() {
        // 真正被消费者监听的队列。订单超时消息最终会流入这里。
        return QueueBuilder.durable(ORDER_TIMEOUT_QUEUE).build();
    }

    @Bean
    public Binding orderDelayBinding() {
        return BindingBuilder.bind(orderDelayQueue()).to(orderExchange()).with(ORDER_DELAY_ROUTING_KEY);
    }

    @Bean
    public Binding orderTimeoutBinding() {
        return BindingBuilder.bind(orderTimeoutQueue()).to(orderExchange()).with(ORDER_TIMEOUT_ROUTING_KEY);
    }
}
