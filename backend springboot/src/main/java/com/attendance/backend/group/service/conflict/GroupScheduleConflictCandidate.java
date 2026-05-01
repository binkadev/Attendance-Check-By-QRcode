package com.attendance.backend.group.service.conflict;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record GroupScheduleConflictCandidate(
        UUID groupId,
        UUID ownerUserId,
        String groupName,
        String courseCode,
        String classCode,
        String campus,
        String room,
        LocalDate startDate,
        LocalDate plannedEndDate,
        String dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {
}
