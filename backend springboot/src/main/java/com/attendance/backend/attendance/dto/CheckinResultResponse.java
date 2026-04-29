package com.attendance.backend.attendance.dto;

import com.attendance.backend.domain.enums.AttendanceStatus;
import com.attendance.backend.domain.enums.CheckInMethod;

import java.time.Instant;
import java.util.UUID;

public record CheckinResultResponse(
        boolean success,
        String title,
        String message,
        UUID sessionId,
        UUID groupId,
        String sessionName,
        String subjectName,
        String groupCode,
        String courseCode,
        String classCode,
        String displayCode,
        Instant checkInAt,
        AttendanceStatus attendanceStatus,
        String attendanceStatusLabel,
        CheckInMethod checkInMethod,
        String room,
        String campus,
        String locationDisplay,
        String locationSubtitle
) {
}