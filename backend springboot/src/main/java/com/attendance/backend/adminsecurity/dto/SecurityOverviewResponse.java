package com.attendance.backend.adminsecurity.dto;

public record SecurityOverviewResponse(
        int hours,
        long passwordResetIssued,
        long passwordResetThrottled,
        long passwordResetMailFailed,
        long loginSuccess,
        long loginInvalidCredentials,
        long loginThrottled,
        long emailOutboxPending,
        long emailOutboxRetry,
        long emailOutboxDead,
        long sessionsRevokedByPasswordReset
) {
}