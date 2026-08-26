package com.gametrade.notification.service;

import com.gametrade.common.notify.NotificationMessage;
import com.gametrade.notification.domain.Notification;
import com.gametrade.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Notification save(NotificationMessage message) {
        Notification notification = new Notification();
        notification.setUserId(message.userId());
        notification.setTitle(message.title());
        notification.setContent(message.content());
        notification.setRefOrderId(message.refOrderId());
        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<Notification> listByUser(Long userId) {
        return notificationRepository.findByUserIdOrderByIdDesc(userId);
    }

    @Transactional
    public void markRead(Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }
}
