package com.attendance.backend.attendance.service;

import com.attendance.backend.attendance.dto.AttendanceSummaryResponse;
import com.attendance.backend.attendance.dto.PageMyAttendanceHistoryResponse;
import com.attendance.backend.attendance.dto.UpcomingSessionsTimelineResponse;

import java.util.UUID;

public interface AttendanceReadService {

    UpcomingSessionsTimelineResponse listUpcomingSessions(UUID actorUserId, int limit);

    AttendanceSummaryResponse getMyAttendanceSummary(
            UUID actorUserId,
            String semester,
            String academicYear
    );

    PageMyAttendanceHistoryResponse listMyAttendancesInGroup(
            UUID actorUserId,
            UUID groupId,
            int page,
            int size
    );

    byte[] exportGroupAttendance(UUID actorUserId, UUID groupId);
}