package com.attendance.backend.attendance.service;

import com.attendance.backend.attendance.dto.CheckinResultResponse;
import com.attendance.backend.attendance.repository.AttendanceSessionRepository;
import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.domain.entity.AttendanceSession;
import com.attendance.backend.domain.entity.GroupMember;
import com.attendance.backend.domain.enums.AttendanceStatus;
import com.attendance.backend.domain.enums.CheckInMethod;
import com.attendance.backend.domain.enums.MemberStatus;
import com.attendance.backend.group.repository.GroupMemberRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@Service
public class CheckinResultServiceImpl implements CheckinResultService {

    private final AttendanceSessionRepository attendanceSessionRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final JdbcTemplate jdbcTemplate;

    public CheckinResultServiceImpl(
            AttendanceSessionRepository attendanceSessionRepository,
            GroupMemberRepository groupMemberRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.attendanceSessionRepository = attendanceSessionRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public CheckinResultResponse getMyCheckinResult(UUID actorUserId, UUID sessionId) {
        AttendanceSession session = attendanceSessionRepository.findActiveById(sessionId)
                .orElseThrow(() -> ApiException.notFound("SESSION_NOT_FOUND", "Session not found"));

        requireApprovedMember(actorUserId, session.getGroupId());

        List<CheckinResultRow> rows = jdbcTemplate.query(
                """
                SELECT
                    BIN_TO_UUID(s.id, 1) AS session_id,
                    BIN_TO_UUID(g.id, 1) AS group_id,
                    COALESCE(s.title, CONCAT('Buổi học ', DATE_FORMAT(s.session_date, '%d/%m/%Y'))) AS session_name,
                    g.name AS subject_name,
                    g.code AS group_code,
                    g.course_code,
                    g.class_code,
                    g.room,
                    g.campus,
                    sa.attendance_status,
                    sa.check_in_at,
                    sa.check_in_method
                FROM attendance_sessions s
                JOIN class_groups g
                  ON g.id = s.group_id
                 AND g.deleted_at IS NULL
                JOIN session_attendance sa
                  ON sa.session_id = s.id
                 AND sa.user_id = UUID_TO_BIN(?, 1)
                WHERE s.id = UUID_TO_BIN(?, 1)
                  AND s.deleted_at IS NULL
                LIMIT 1
                """,
                (rs, rowNum) -> new CheckinResultRow(
                        UUID.fromString(rs.getString("session_id")),
                        UUID.fromString(rs.getString("group_id")),
                        rs.getString("session_name"),
                        rs.getString("subject_name"),
                        rs.getString("group_code"),
                        rs.getString("course_code"),
                        rs.getString("class_code"),
                        rs.getString("room"),
                        rs.getString("campus"),
                        AttendanceStatus.valueOf(rs.getString("attendance_status")),
                        toInstant(rs.getObject("check_in_at")),
                        rs.getString("check_in_method") == null
                                ? null
                                : CheckInMethod.valueOf(rs.getString("check_in_method"))
                ),
                actorUserId.toString(),
                sessionId.toString()
        );

        if (rows.isEmpty()) {
            throw ApiException.notFound(
                    "CHECKIN_RESULT_NOT_FOUND",
                    "Check-in result not found for this session/user"
            );
        }

        CheckinResultRow row = rows.get(0);

        if (row.checkInAt() == null) {
            throw ApiException.notFound(
                    "CHECKIN_RESULT_NOT_FOUND",
                    "User has not checked in for this session"
            );
        }

        if (row.attendanceStatus() == AttendanceStatus.ABSENT) {
            throw ApiException.conflict(
                    "USER_NOT_CHECKED_IN",
                    "User is marked ABSENT for this session"
            );
        }

        String displayCode = firstNonBlank(row.groupCode(), row.classCode(), row.courseCode());
        String locationDisplay = buildLocationDisplay(row.room(), row.campus());

        return new CheckinResultResponse(
                true,
                "Điểm danh thành công!",
                "Bạn đã được ghi nhận có mặt tại lớp.",
                row.sessionId(),
                row.groupId(),
                row.sessionName(),
                row.subjectName(),
                row.groupCode(),
                row.courseCode(),
                row.classCode(),
                displayCode,
                row.checkInAt(),
                row.attendanceStatus(),
                attendanceStatusLabel(row.attendanceStatus()),
                row.checkInMethod(),
                row.room(),
                row.campus(),
                locationDisplay,
                row.campus()
        );
    }

    private void requireApprovedMember(UUID actorUserId, UUID groupId) {
        GroupMember membership = groupMemberRepository.findByGroupIdAndUserId(groupId, actorUserId)
                .orElseThrow(() -> ApiException.forbidden(
                        "FORBIDDEN",
                        "You are not a member of this group"
                ));

        if (membership.getMemberStatus() != MemberStatus.APPROVED) {
            throw ApiException.forbidden(
                    "FORBIDDEN",
                    "Your group membership is not approved"
            );
        }
    }

    private String attendanceStatusLabel(AttendanceStatus status) {
        return switch (status) {
            case PRESENT -> "Đúng giờ";
            case LATE -> "Đi muộn";
            case ABSENT -> "Vắng";
            case EXCUSED -> "Có phép";
        };
    }

    private String buildLocationDisplay(String room, String campus) {
        String safeRoom = normalize(room);
        String safeCampus = normalize(campus);

        if (safeRoom == null && safeCampus == null) {
            return null;
        }

        if (safeRoom == null) {
            return safeCampus;
        }

        if (safeCampus == null) {
            return "Phòng " + safeRoom;
        }

        return "Phòng " + safeRoom + " - " + safeCampus;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = normalize(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Instant toInstant(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Instant instant) {
            return instant;
        }

        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }

        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        }

        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }

        if (value instanceof ZonedDateTime zonedDateTime) {
            return zonedDateTime.toInstant();
        }

        if (value instanceof java.util.Date date) {
            return date.toInstant();
        }

        String text = value.toString().trim();

        try {
            return Instant.parse(text);
        } catch (DateTimeParseException ignored) {
            return LocalDateTime.parse(text)
                    .atZone(ZoneId.systemDefault())
                    .toInstant();
        }
    }

    private record CheckinResultRow(
            UUID sessionId,
            UUID groupId,
            String sessionName,
            String subjectName,
            String groupCode,
            String courseCode,
            String classCode,
            String room,
            String campus,
            AttendanceStatus attendanceStatus,
            Instant checkInAt,
            CheckInMethod checkInMethod
    ) {
    }
}