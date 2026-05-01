package com.attendance.backend.group.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record GroupScheduleConflictItemResponse(
        String type,
        UUID groupId,
        String groupName,
        String courseCode,
        String classCode,
        String campus,
        String room,
        String dayOfWeek,
        LocalDate overlapFrom,
        LocalDate overlapTo,
        LocalTime requestedStartTime,
        LocalTime requestedEndTime,
        LocalTime existingStartTime,
        LocalTime existingEndTime
) {
}
