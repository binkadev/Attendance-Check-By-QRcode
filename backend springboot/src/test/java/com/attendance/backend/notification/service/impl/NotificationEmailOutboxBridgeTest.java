package com.attendance.backend.notification.service.impl;

import com.attendance.backend.auth.repository.UserRepository;
import com.attendance.backend.domain.entity.Notification;
import com.attendance.backend.domain.entity.NotificationDelivery;
import com.attendance.backend.domain.entity.User;
import com.attendance.backend.domain.enums.NotificationSeverity;
import com.attendance.backend.domain.enums.NotificationType;
import com.attendance.backend.mail.EmailOutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NotificationEmailOutboxBridgeTest {

    private UserRepository userRepository;
    private EmailOutboxService emailOutboxService;
    private NotificationEmailOutboxBridge bridge;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        emailOutboxService = mock(EmailOutboxService.class);
        bridge = new NotificationEmailOutboxBridge(userRepository, emailOutboxService);
    }

    @Test
    void enqueue_shouldCreateEmailOutboxRowAndReturnPersistedId() {
        UUID recipientUserId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        UUID outboxId = UUID.randomUUID();

        User user = new User();
        user.setId(recipientUserId);
        user.setEmail("student@example.test");

        Notification notification = new Notification();
        notification.setRecipientUserId(recipientUserId);
        notification.setType(NotificationType.CHECKIN_SUCCESS);
        notification.setSeverity(NotificationSeverity.INFO);
        notification.setTitle("Check-in success");
        notification.setBody("You checked in successfully");

        NotificationDelivery delivery = new NotificationDelivery();
        delivery.setId(deliveryId);
        delivery.setNotificationId(UUID.randomUUID());

        when(userRepository.findByIdAndDeletedAtIsNull(recipientUserId)).thenReturn(Optional.of(user));
        when(emailOutboxService.enqueueNotificationEmail(
                deliveryId,
                "student@example.test",
                "Check-in success",
                "Check-in success",
                "You checked in successfully"
        )).thenReturn(outboxId);

        UUID actual = bridge.enqueue(notification, delivery);

        assertEquals(outboxId, actual);
        verify(emailOutboxService).enqueueNotificationEmail(
                deliveryId,
                "student@example.test",
                "Check-in success",
                "Check-in success",
                "You checked in successfully"
        );
    }

    @Test
    void enqueue_shouldThrowWithoutOutboxIdWhenRecipientMissing() {
        UUID recipientUserId = UUID.randomUUID();

        Notification notification = new Notification();
        notification.setRecipientUserId(recipientUserId);

        NotificationDelivery delivery = new NotificationDelivery();
        delivery.setId(UUID.randomUUID());

        when(userRepository.findByIdAndDeletedAtIsNull(recipientUserId)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> bridge.enqueue(notification, delivery));
        verifyNoInteractions(emailOutboxService);
    }
}
