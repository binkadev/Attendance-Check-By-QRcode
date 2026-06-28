package com.attendance.backend.notification.service.impl;

import com.attendance.backend.domain.entity.Notification;
import com.attendance.backend.domain.entity.NotificationDelivery;
import com.attendance.backend.domain.enums.NotificationChannel;
import com.attendance.backend.domain.enums.NotificationDeliveryStatus;
import com.attendance.backend.mail.EmailOutboxRepository;
import com.attendance.backend.notification.repository.NotificationDeliveryRepository;
import com.attendance.backend.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationDispatchServiceImplTest {

    private NotificationDeliveryRepository notificationDeliveryRepository;
    private NotificationRepository notificationRepository;
    private NotificationEmailOutboxBridge notificationEmailOutboxBridge;
    private EmailOutboxRepository emailOutboxRepository;
    private NotificationDispatchServiceImpl service;

    @BeforeEach
    void setUp() {
        notificationDeliveryRepository = mock(NotificationDeliveryRepository.class);
        notificationRepository = mock(NotificationRepository.class);
        notificationEmailOutboxBridge = mock(NotificationEmailOutboxBridge.class);
        emailOutboxRepository = mock(EmailOutboxRepository.class);
        service = new NotificationDispatchServiceImpl(
                notificationDeliveryRepository,
                notificationRepository,
                notificationEmailOutboxBridge,
                emailOutboxRepository,
                Clock.fixed(Instant.parse("2026-06-28T00:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void processOne_shouldRetryWithoutWritingInvalidEmailOutboxId() {
        UUID deliveryId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        UUID fakeOutboxId = UUID.randomUUID();

        NotificationDelivery delivery = new NotificationDelivery();
        delivery.setId(deliveryId);
        delivery.setNotificationId(notificationId);
        delivery.setChannel(NotificationChannel.EMAIL);
        delivery.setStatus(NotificationDeliveryStatus.PENDING);
        delivery.setRetryCount(0);
        delivery.setNextAttemptAt(Instant.parse("2026-06-28T00:00:00Z"));

        Notification notification = new Notification();
        notification.setId(notificationId);

        when(notificationDeliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationEmailOutboxBridge.enqueue(notification, delivery)).thenReturn(fakeOutboxId);
        when(emailOutboxRepository.existsById(fakeOutboxId)).thenReturn(false);

        boolean processed = service.processOne(deliveryId);

        assertFalse(processed);
        assertEquals(NotificationDeliveryStatus.RETRY, delivery.getStatus());
        assertNull(delivery.getEmailOutboxId());
        assertEquals("DISPATCH_FAILED", delivery.getLastErrorCode());
        verify(notificationDeliveryRepository, times(2)).save(delivery);
    }
}
