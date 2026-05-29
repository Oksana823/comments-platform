package com.hmdp.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    // 1. 声明交换机
    @Bean
    public DirectExchange seckillExchange() {
        return new DirectExchange("seckill.direct", true, false);
    }

    // 2. 声明队列
    @Bean
    public Queue seckillQueue() {
        return new Queue("seckill.order.queue", true);
    }

    // 3. 绑定
    @Bean
    public Binding seckillBinding() {
        return BindingBuilder
                .bind(seckillQueue())
                .to(seckillExchange())
                .with("seckill.order");
    }

    // 4. JSON 序列化（替代 Java 默认序列化）
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
