package com.gametrade.notification.messaging;

import com.gametrade.common.notify.NotificationMessage;
import com.gametrade.common.notify.NotificationRabbit;
import com.gametrade.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes notification messages from RabbitMQ and persists them to the inbox.
 */
@Component
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    private final NotificationService notificationService;

    public NotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = NotificationRabbit.QUEUE)
    public void onMessage(NotificationMessage message) {
        log.info("notification received for user {} (order {})", message.userId(), message.refOrderId());
        notificationService.save(message);
    }
}
