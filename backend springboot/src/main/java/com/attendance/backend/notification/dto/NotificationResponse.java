package com.attendance.backend.notification.dto;

import com.attendance.backend.domain.enums.NotificationSeverity;
import com.attendance.backend.domain.enums.NotificationSourceType;
import com.attendance.backend.domain.enums.NotificationType;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID groupId,
        UUID sessionId,
        NotificationType type,
        String title,
        String body,
        JsonNode payload,
        NotificationSeverity severity,
        NotificationSourceType sourceType,
        UUID sourceRefId,
        boolean isRead,
        Instant readAt,
        Instant createdAt
) {
}