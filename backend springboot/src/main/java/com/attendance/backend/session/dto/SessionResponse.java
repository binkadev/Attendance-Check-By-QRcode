package com.attendance.backend.session.dto;

import com.attendance.backend.domain.enums.SessionStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SessionResponse(
        UUID id,
        UUID groupId,
        UUID createdByUserId,
        String title,
        LocalDate sessionDate,
        SessionStatus status,
        Instant startAt,
        Instant endAt,
        Instant checkinOpenAt,
        Instant checkinCloseAt,
        Integer timeWindowMinutes,
        Integer lateAfterMinutes,
        Integer qrRotateSeconds,
        Boolean allowManualOverride,
        String note,
        Instant createdAt,
        Instant updatedAt
) {
}