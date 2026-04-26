package com.attendance.backend.attendance.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MyAttendanceHistoryItemResponse(
        UUID sessionId,
        UUID groupId,
        String groupName,
        String sessionName,
        LocalDate sessionDate,
        Instant startTime,
        Instant endTime,
        String sessionStatus,
        String attendanceStatus,
        Instant checkInAt,
        String checkInMethod,
        Boolean suspiciousFlag,
        String suspiciousReason
) {
}