package com.gametrade.common.notify;

/**
 * RabbitMQ topology names for the notification pipeline.
 */
public final class NotificationRabbit {

    public static final String EXCHANGE = "notification.exchange";
    public static final String QUEUE = "notification.queue";
    public static final String ROUTING_KEY = "notification.order";

    private NotificationRabbit() {
    }
}
