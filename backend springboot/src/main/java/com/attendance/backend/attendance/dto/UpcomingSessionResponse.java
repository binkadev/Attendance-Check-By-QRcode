package com.attendance.backend.attendance.dto;

import java.time.Instant;
import java.util.UUID;

public record UpcomingSessionResponse(
        UUID sessionId,
        UUID groupId,
        String sessionName,
        Instant startTime,
        Instant endTime,
        String room,
        String groupName
) {
}