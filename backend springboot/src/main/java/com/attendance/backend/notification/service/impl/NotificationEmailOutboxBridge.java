package com.attendance.backend.notification.service.impl;

import com.attendance.backend.auth.repository.UserRepository;
import com.attendance.backend.domain.entity.Notification;
import com.attendance.backend.domain.entity.NotificationDelivery;
import com.attendance.backend.domain.entity.User;
import com.attendance.backend.mail.EmailOutboxService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class NotificationEmailOutboxBridge {

    private final UserRepository userRepository;
    private final EmailOutboxService emailOutboxService;

    public NotificationEmailOutboxBridge(
            UserRepository userRepository,
            EmailOutboxService emailOutboxService
    ) {
        this.userRepository = userRepository;
        this.emailOutboxService = emailOutboxService;
    }

    public UUID enqueue(Notification notification, NotificationDelivery delivery) {
        if (notification == null) {
            throw new IllegalStateException("Notification is required for email dispatch");
        }
        if (delivery == null || delivery.getId() == null) {
            throw new IllegalStateException("Notification delivery is required for email dispatch");
        }
        if (notification.getRecipientUserId() == null) {
            throw new IllegalStateException("Notification recipient is required for email dispatch");
        }

        User recipient = userRepository.findByIdAndDeletedAtIsNull(notification.getRecipientUserId())
                .orElseThrow(() -> new IllegalStateException("Notification recipient does not exist"));

        String toEmail = recipient.getEmail();
        if (toEmail == null || toEmail.isBlank()) {
            throw new IllegalStateException("Notification recipient email is not available");
        }
        toEmail = toEmail.trim();

        return emailOutboxService.enqueueNotificationEmail(
                delivery.getId(),
                toEmail,
                notification.getTitle(),
                notification.getTitle(),
                notification.getBody()
        );
    }
}
