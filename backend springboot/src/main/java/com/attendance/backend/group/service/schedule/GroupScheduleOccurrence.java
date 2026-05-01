package com.attendance.backend.group.service.schedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record GroupScheduleOccurrence(
        int sessionIndex,
        LocalDate sessionDate,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String room,
        String campus
) {
}
