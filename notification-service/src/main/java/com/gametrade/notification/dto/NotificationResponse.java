package com.gametrade.notification.dto;

import com.gametrade.notification.domain.Notification;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        String title,
        String content,
        Long refOrderId,
        boolean read,
        Instant createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getTitle(), n.getContent(), n.getRefOrderId(), n.isRead(), n.getCreatedAt());
    }
}
