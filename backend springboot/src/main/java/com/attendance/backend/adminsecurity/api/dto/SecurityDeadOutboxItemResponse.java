package com.attendance.backend.adminsecurity.api.dto;

import java.time.Instant;
import java.util.UUID;

public record SecurityDeadOutboxItemResponse(
        UUID id,
        String toEmail,
        String status,
        int retryCount,
        String lastErrorCode,
        String lastErrorMessage,
        Instant nextAttemptAt,
        Instant createdAt
) {
}