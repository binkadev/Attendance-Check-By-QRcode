package com.attendance.backend.group.service.schedule;

import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.group.dto.GroupWeeklyScheduleRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Component
public class GroupSchedulePlanner {

    private static final int MAX_LOOKAHEAD_DAYS = 3660;

    public LocalDate calculatePlannedEndDate(
            LocalDate startDate,
            List<GroupWeeklyScheduleRequest> weeklySchedules,
            Integer totalSessions,
            String defaultCampus,
            String defaultRoom
    ) {
        List<GroupScheduleOccurrence> occurrences = calculatePlannedOccurrences(
                startDate,
                weeklySchedules,
                totalSessions,
                defaultCampus,
                defaultRoom
        );
        return occurrences.get(occurrences.size() - 1).sessionDate();
    }

    public List<GroupScheduleOccurrence> calculatePlannedOccurrences(
            LocalDate startDate,
            List<GroupWeeklyScheduleRequest> weeklySchedules,
            Integer totalSessions,
            String defaultCampus,
            String defaultRoom
    ) {
        if (startDate == null) {
            throw ApiException.unprocessable("START_DATE_REQUIRED", "startDate is required");
        }
        if (weeklySchedules == null || weeklySchedules.isEmpty()) {
            throw ApiException.unprocessable("WEEKLY_SCHEDULE_REQUIRED", "weeklySchedules must not be empty");
        }
        if (totalSessions == null || totalSessions <= 0) {
            throw ApiException.unprocessable("TOTAL_SESSIONS_INVALID", "totalSessions must be greater than 0");
        }

        List<NormalizedSchedule> normalizedSchedules = weeklySchedules.stream()
                .map(this::normalizeSchedule)
                .sorted(Comparator
                        .comparingInt((NormalizedSchedule item) -> item.dayOfWeek().getValue())
                        .thenComparing(NormalizedSchedule::startTime)
                        .thenComparing(NormalizedSchedule::endTime))
                .toList();

        boolean startDateMatchesSchedule = normalizedSchedules.stream()
                .anyMatch(item -> item.dayOfWeek() == startDate.getDayOfWeek());
        if (!startDateMatchesSchedule) {
            throw ApiException.unprocessable(
                    "START_DATE_NOT_MATCH_SCHEDULE",
                    "startDate must match at least one weeklySchedules.dayOfWeek"
            );
        }

        List<GroupScheduleOccurrence> occurrences = new ArrayList<>();
        LocalDate cursor = startDate;
        int guard = 0;
        while (occurrences.size() < totalSessions && guard <= MAX_LOOKAHEAD_DAYS) {
            LocalDate currentDate = cursor;
            List<NormalizedSchedule> schedulesOfDay = normalizedSchedules.stream()
                    .filter(item -> item.dayOfWeek() == currentDate.getDayOfWeek())
                    .toList();

            for (NormalizedSchedule item : schedulesOfDay) {
                if (occurrences.size() >= totalSessions) {
                    break;
                }
                int sessionIndex = occurrences.size() + 1;
                occurrences.add(new GroupScheduleOccurrence(
                        sessionIndex,
                        currentDate,
                        item.dayOfWeek(),
                        item.startTime(),
                        item.endTime(),
                        currentDate.atTime(item.startTime()),
                        currentDate.atTime(item.endTime()),
                        defaultRoom,
                        defaultCampus
                ));
            }
            cursor = cursor.plusDays(1);
            guard++;
        }

        if (occurrences.size() != totalSessions) {
            throw ApiException.unprocessable(
                    "SCHEDULE_GENERATION_FAILED",
                    "Cannot generate enough planned sessions from weeklySchedules"
            );
        }
        return occurrences;
    }

    private NormalizedSchedule normalizeSchedule(GroupWeeklyScheduleRequest item) {
        if (item == null) {
            throw ApiException.unprocessable("INVALID_WEEKLY_SCHEDULE", "weeklySchedules contains invalid item");
        }
        DayOfWeek dayOfWeek = parseDayOfWeek(item.getDayOfWeek());
        LocalTime startTime = parseLocalTime(item.getStartTime(), "startTime");
        LocalTime endTime = parseLocalTime(item.getEndTime(), "endTime");
        if (!startTime.isBefore(endTime)) {
            throw ApiException.unprocessable("INVALID_WEEKLY_SCHEDULE_TIME", "startTime must be earlier than endTime");
        }
        return new NormalizedSchedule(dayOfWeek, startTime, endTime);
    }

    private DayOfWeek parseDayOfWeek(String value) {
        if (!StringUtils.hasText(value)) {
            throw ApiException.unprocessable("INVALID_DAY_OF_WEEK", "dayOfWeek is required");
        }
        try {
            return DayOfWeek.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw ApiException.unprocessable("INVALID_DAY_OF_WEEK", "dayOfWeek is invalid");
        }
    }

    private LocalTime parseLocalTime(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw ApiException.unprocessable("INVALID_WEEKLY_SCHEDULE_TIME", fieldName + " is required");
        }
        try {
            return LocalTime.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw ApiException.unprocessable("INVALID_WEEKLY_SCHEDULE_TIME", fieldName + " must be HH:mm");
        }
    }

    private record NormalizedSchedule(
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime
    ) {
    }
}
