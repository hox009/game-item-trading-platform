package com.gametrade.order.config;

import com.gametrade.common.notify.NotificationRabbit;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the notification exchange and JSON message conversion for RabbitMQ.
 */
@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NotificationRabbit.EXCHANGE, true, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
