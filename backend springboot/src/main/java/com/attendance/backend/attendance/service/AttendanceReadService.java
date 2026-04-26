package com.attendance.backend.attendance.service;

import com.attendance.backend.attendance.dto.PageMyAttendanceHistoryResponse;
import com.attendance.backend.attendance.dto.UpcomingSessionResponse;

import java.util.List;
import java.util.UUID;

public interface AttendanceReadService {

    List<UpcomingSessionResponse> listUpcomingSessions(UUID actorUserId, int limit);

    PageMyAttendanceHistoryResponse listMyAttendancesInGroup(
            UUID actorUserId,
            UUID groupId,
            int page,
            int size
    );

    byte[] exportGroupAttendance(UUID actorUserId, UUID groupId);
}