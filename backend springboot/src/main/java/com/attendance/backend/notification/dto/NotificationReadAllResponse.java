package com.attendance.backend.notification.dto;

import java.time.Instant;

public record NotificationReadAllResponse(
        int updatedCount,
        Instant readAt
) {
}