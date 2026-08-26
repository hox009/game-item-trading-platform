package com.gametrade.common.event;

/**
 * Kafka topic names for order lifecycle events. Shared by producers
 * (order-service) and consumers (inventory-service, notification-service).
 */
public final class OrderTopics {

    public static final String ORDER_PAID = "order.paid";
    public static final String ORDER_CANCELLED = "order.cancelled";

    private OrderTopics() {
    }
}
