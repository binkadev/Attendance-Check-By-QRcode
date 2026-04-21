package com.attendance.backend.group.dto;

import com.attendance.backend.domain.enums.ApprovalMode;
import com.attendance.backend.domain.enums.GroupStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GroupResponse(
        UUID id,
        UUID ownerUserId,
        String name,
        String code,
        String courseCode,
        String classCode,
        String joinCode,
        String description,
        String semester,
        String academicYear,
        String campus,
        String room,
        Integer totalSessions,
        Integer maxAllowedAbsences,
        List<GroupWeeklyScheduleResponse> weeklySchedules,
        ApprovalMode approvalMode,
        boolean allowAutoJoinOnCheckin,
        GroupStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}