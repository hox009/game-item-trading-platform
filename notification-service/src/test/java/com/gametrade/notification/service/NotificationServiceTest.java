package com.gametrade.notification.service;

import com.gametrade.common.notify.NotificationMessage;
import com.gametrade.notification.domain.Notification;
import com.gametrade.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    private NotificationRepository repository;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        repository = mock(NotificationRepository.class);
        service = new NotificationService(repository);
        when(repository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void save_mapsMessageFields() {
        Notification saved = service.save(new NotificationMessage(5L, "Paid", "Order #9 paid", 9L));

        assertThat(saved.getUserId()).isEqualTo(5L);
        assertThat(saved.getTitle()).isEqualTo("Paid");
        assertThat(saved.getRefOrderId()).isEqualTo(9L);
        assertThat(saved.isRead()).isFalse();
    }
}
