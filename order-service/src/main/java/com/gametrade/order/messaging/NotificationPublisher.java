package com.gametrade.order.messaging;

import com.gametrade.common.notify.NotificationMessage;
import com.gametrade.common.notify.NotificationRabbit;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Sends user-facing notifications to RabbitMQ for asynchronous delivery.
 */
@Component
public class NotificationPublisher {

    private final RabbitTemplate rabbitTemplate;

    public NotificationPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void send(NotificationMessage message) {
        rabbitTemplate.convertAndSend(
                NotificationRabbit.EXCHANGE,
                NotificationRabbit.ROUTING_KEY,
                message);
    }
}
