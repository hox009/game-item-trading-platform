package com.gametrade.order.messaging;

import com.gametrade.common.event.OrderCancelledEvent;
import com.gametrade.common.event.OrderPaidEvent;
import com.gametrade.common.event.OrderTopics;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes order lifecycle events to Kafka. Keyed by order id so events for
 * the same order stay ordered within a partition.
 */
@Component
public class OrderEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishPaid(OrderPaidEvent event) {
        kafkaTemplate.send(OrderTopics.ORDER_PAID, String.valueOf(event.orderId()), event);
    }

    public void publishCancelled(OrderCancelledEvent event) {
        kafkaTemplate.send(OrderTopics.ORDER_CANCELLED, String.valueOf(event.orderId()), event);
    }
}
