package com.attendance.backend.notification.dto;

import com.attendance.backend.domain.enums.NotificationChannel;
import com.attendance.backend.domain.enums.NotificationDeliveryStatus;
import com.attendance.backend.domain.enums.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationDeliveryAdminResponse(
        UUID id,
        UUID notificationId,
        UUID recipientUserId,
        UUID groupId,
        NotificationType type,
        NotificationChannel channel,
        NotificationDeliveryStatus status,
        UUID emailOutboxId,
        int retryCount,
        Instant nextAttemptAt,
        Instant lockedAt,
        Instant processedAt,
        String lastErrorCode,
        String lastErrorMessage,
        Instant createdAt,
        Instant updatedAt
) {
}