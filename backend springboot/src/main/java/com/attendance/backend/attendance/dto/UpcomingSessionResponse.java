package com.attendance.backend.attendance.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record UpcomingSessionResponse(
        UUID attendanceSessionId,
        String attendanceStatus,
        Instant checkinOpenAt,
        Instant checkinCloseAt,
        UUID groupId,
        String sessionName,
        Instant startAt,
        Instant endAt,
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime,
        String room,
        String groupName
) {
}