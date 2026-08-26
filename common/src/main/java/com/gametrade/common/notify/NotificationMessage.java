package com.gametrade.common.notify;

import java.io.Serializable;

/**
 * A user-facing notification carried over RabbitMQ from producers
 * (e.g. order-service) to notification-service.
 */
public record NotificationMessage(
        Long userId,
        String title,
        String content,
        Long refOrderId
) implements Serializable {
}
