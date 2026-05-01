package com.attendance.backend.group.service.conflict;

import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.domain.enums.GroupStatus;
import com.attendance.backend.group.dto.GroupScheduleConflictItemResponse;
import com.attendance.backend.group.dto.GroupScheduleConflictResponse;
import com.attendance.backend.group.dto.GroupWeeklyScheduleRequest;
import com.attendance.backend.group.repository.GroupWeeklyScheduleRepository;
import com.attendance.backend.group.service.schedule.GroupSchedulePlanner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class GroupScheduleConflictServiceImpl implements GroupScheduleConflictService {

    private static final String TYPE_LECTURER = "LECTURER";
    private static final String TYPE_ROOM = "ROOM";

    private final GroupWeeklyScheduleRepository groupWeeklyScheduleRepository;
    private final GroupSchedulePlanner groupSchedulePlanner;

    public GroupScheduleConflictServiceImpl(
            GroupWeeklyScheduleRepository groupWeeklyScheduleRepository,
            GroupSchedulePlanner groupSchedulePlanner
    ) {
        this.groupWeeklyScheduleRepository = groupWeeklyScheduleRepository;
        this.groupSchedulePlanner = groupSchedulePlanner;
    }

    @Override
    @Transactional(readOnly = true)
    public GroupScheduleConflictResponse validateSchedule(
            UUID ownerUserId,
            UUID excludeGroupId,
            LocalDate startDate,
            List<GroupWeeklyScheduleRequest> weeklySchedules,
            Integer totalSessions,
            String campus,
            String room
    ) {
        LocalDate plannedEndDate = groupSchedulePlanner.calculatePlannedEndDate(
                startDate,
                weeklySchedules,
                totalSessions,
                campus,
                room
        );

        List<GroupScheduleConflictCandidate> candidates =
                groupWeeklyScheduleRepository.findActiveScheduleCandidatesOverlappingDateRange(
                        excludeGroupId,
                        startDate,
                        plannedEndDate,
                        GroupStatus.ACTIVE
                );

        String normalizedCampus = normalizeNullable(campus);
        String normalizedRoom = normalizeNullable(room);
        List<GroupScheduleConflictItemResponse> conflicts = new ArrayList<>();

        for (GroupWeeklyScheduleRequest requested : weeklySchedules) {
            String requestedDayOfWeek = normalizeRequired(requested.getDayOfWeek());
            LocalTime requestedStartTime = LocalTime.parse(requested.getStartTime());
            LocalTime requestedEndTime = LocalTime.parse(requested.getEndTime());

            for (GroupScheduleConflictCandidate candidate : candidates) {
                if (!requestedDayOfWeek.equalsIgnoreCase(candidate.dayOfWeek())) {
                    continue;
                }
                if (!timeRangesOverlap(requestedStartTime, requestedEndTime, candidate.startTime(), candidate.endTime())) {
                    continue;
                }

                LocalDate overlapFrom = max(startDate, candidate.startDate());
                LocalDate overlapTo = min(plannedEndDate, candidate.plannedEndDate());

                if (ownerUserId != null && ownerUserId.equals(candidate.ownerUserId())) {
                    conflicts.add(toConflict(
                            TYPE_LECTURER,
                            candidate,
                            requestedDayOfWeek,
                            overlapFrom,
                            overlapTo,
                            requestedStartTime,
                            requestedEndTime
                    ));
                }

                if (hasRoom(normalizedCampus, normalizedRoom)
                        && roomMatches(normalizedCampus, normalizedRoom, candidate.campus(), candidate.room())) {
                    conflicts.add(toConflict(
                            TYPE_ROOM,
                            candidate,
                            requestedDayOfWeek,
                            overlapFrom,
                            overlapTo,
                            requestedStartTime,
                            requestedEndTime
                    ));
                }
            }
        }

        List<GroupScheduleConflictItemResponse> sortedConflicts = conflicts.stream()
                .sorted(Comparator
                        .comparing(GroupScheduleConflictItemResponse::type)
                        .thenComparing(GroupScheduleConflictItemResponse::overlapFrom)
                        .thenComparing(GroupScheduleConflictItemResponse::dayOfWeek)
                        .thenComparing(GroupScheduleConflictItemResponse::requestedStartTime)
                        .thenComparing(item -> item.groupName() == null ? "" : item.groupName()))
                .toList();

        return new GroupScheduleConflictResponse(sortedConflicts.isEmpty(), sortedConflicts);
    }

    @Override
    @Transactional(readOnly = true)
    public void assertNoConflict(
            UUID ownerUserId,
            UUID excludeGroupId,
            LocalDate startDate,
            List<GroupWeeklyScheduleRequest> weeklySchedules,
            Integer totalSessions,
            String campus,
            String room
    ) {
        GroupScheduleConflictResponse result = validateSchedule(
                ownerUserId,
                excludeGroupId,
                startDate,
                weeklySchedules,
                totalSessions,
                campus,
                room
        );

        if (!result.valid()) {
            throw ApiException.conflict(
                    "SCHEDULE_CONFLICT",
                    "Schedule conflicts with existing class schedule"
            );
        }
    }

    private GroupScheduleConflictItemResponse toConflict(
            String type,
            GroupScheduleConflictCandidate candidate,
            String requestedDayOfWeek,
            LocalDate overlapFrom,
            LocalDate overlapTo,
            LocalTime requestedStartTime,
            LocalTime requestedEndTime
    ) {
        return new GroupScheduleConflictItemResponse(
                type,
                candidate.groupId(),
                candidate.groupName(),
                candidate.courseCode(),
                candidate.classCode(),
                candidate.campus(),
                candidate.room(),
                requestedDayOfWeek,
                overlapFrom,
                overlapTo,
                requestedStartTime,
                requestedEndTime,
                candidate.startTime(),
                candidate.endTime()
        );
    }

    private boolean timeRangesOverlap(LocalTime requestedStart, LocalTime requestedEnd, LocalTime existingStart, LocalTime existingEnd) {
        return requestedStart.isBefore(existingEnd) && existingStart.isBefore(requestedEnd);
    }

    private boolean hasRoom(String campus, String room) {
        return StringUtils.hasText(campus) && StringUtils.hasText(room);
    }

    private boolean roomMatches(String campus, String room, String existingCampus, String existingRoom) {
        return normalizeRequired(campus).equalsIgnoreCase(normalizeNullable(existingCampus))
                && normalizeRequired(room).equalsIgnoreCase(normalizeNullable(existingRoom));
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeRequired(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private LocalDate max(LocalDate first, LocalDate second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.isAfter(second) ? first : second;
    }

    private LocalDate min(LocalDate first, LocalDate second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.isBefore(second) ? first : second;
    }
}
