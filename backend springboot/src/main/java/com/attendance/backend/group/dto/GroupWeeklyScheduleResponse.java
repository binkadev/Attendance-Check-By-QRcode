package com.attendance.backend.group.dto;

public record GroupWeeklyScheduleResponse(
        String dayOfWeek,
        String startTime,
        String endTime
) {
}