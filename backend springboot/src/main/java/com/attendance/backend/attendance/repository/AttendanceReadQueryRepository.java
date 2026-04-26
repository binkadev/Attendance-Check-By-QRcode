package com.attendance.backend.attendance.repository;

import com.attendance.backend.attendance.dto.MyAttendanceHistoryItemResponse;
import com.attendance.backend.attendance.dto.UpcomingSessionResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@Repository
public class AttendanceReadQueryRepository {

    private final JdbcTemplate jdbcTemplate;

    public AttendanceReadQueryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<UpcomingSessionResponse> findUpcomingSessions(UUID actorUserId, int limit) {
        return jdbcTemplate.query(
                """
                SELECT
                    BIN_TO_UUID(s.id, 1) AS session_id,
                    BIN_TO_UUID(g.id, 1) AS group_id,
                    COALESCE(s.title, CONCAT('Buổi học ', DATE_FORMAT(s.session_date, '%d/%m/%Y'))) AS session_name,
                    s.start_at,
                    s.end_at,
                    g.room,
                    g.name AS group_name
                FROM attendance_sessions s
                JOIN class_groups g
                  ON g.id = s.group_id
                 AND g.deleted_at IS NULL
                JOIN group_members gm
                  ON gm.group_id = g.id
                 AND gm.user_id = UUID_TO_BIN(?, 1)
                 AND gm.member_status = 'APPROVED'
                WHERE s.deleted_at IS NULL
                  AND s.status <> 'CANCELLED'
                  AND s.start_at >= CURRENT_TIMESTAMP
                ORDER BY s.start_at ASC
                LIMIT ?
                """,
                (rs, rowNum) -> new UpcomingSessionResponse(
                        UUID.fromString(rs.getString("session_id")),
                        UUID.fromString(rs.getString("group_id")),
                        rs.getString("session_name"),
                        toInstant(rs.getObject("start_at")),
                        toInstant(rs.getObject("end_at")),
                        rs.getString("room"),
                        rs.getString("group_name")
                ),
                actorUserId.toString(),
                limit
        );
    }

    public List<MyAttendanceHistoryItemResponse> findMyAttendancesInGroup(
            UUID actorUserId,
            UUID groupId,
            int page,
            int size
    ) {
        int offset = page * size;

        return jdbcTemplate.query(
                """
                SELECT
                    BIN_TO_UUID(s.id, 1) AS session_id,
                    BIN_TO_UUID(g.id, 1) AS group_id,
                    g.name AS group_name,
                    COALESCE(s.title, CONCAT('Buổi học ', DATE_FORMAT(s.session_date, '%d/%m/%Y'))) AS session_name,
                    s.session_date,
                    s.start_at,
                    s.end_at,
                    s.status AS session_status,
                    COALESCE(sa.attendance_status, 'ABSENT') AS attendance_status,
                    sa.check_in_at,
                    sa.check_in_method,
                    COALESCE(sa.suspicious_flag, false) AS suspicious_flag,
                    sa.suspicious_reason
                FROM attendance_sessions s
                JOIN class_groups g
                  ON g.id = s.group_id
                 AND g.deleted_at IS NULL
                LEFT JOIN session_attendance sa
                  ON sa.session_id = s.id
                 AND sa.user_id = UUID_TO_BIN(?, 1)
                WHERE s.group_id = UUID_TO_BIN(?, 1)
                  AND s.deleted_at IS NULL
                  AND s.status <> 'CANCELLED'
                  AND s.start_at <= CURRENT_TIMESTAMP
                ORDER BY s.session_date DESC, s.start_at DESC
                LIMIT ?
                OFFSET ?
                """,
                (rs, rowNum) -> new MyAttendanceHistoryItemResponse(
                        UUID.fromString(rs.getString("session_id")),
                        UUID.fromString(rs.getString("group_id")),
                        rs.getString("group_name"),
                        rs.getString("session_name"),
                        toLocalDate(rs.getObject("session_date")),
                        toInstant(rs.getObject("start_at")),
                        toInstant(rs.getObject("end_at")),
                        rs.getString("session_status"),
                        rs.getString("attendance_status"),
                        toInstant(rs.getObject("check_in_at")),
                        rs.getString("check_in_method"),
                        rs.getBoolean("suspicious_flag"),
                        rs.getString("suspicious_reason")
                ),
                actorUserId.toString(),
                groupId.toString(),
                size,
                offset
        );
    }

    public long countMyAttendancesInGroup(UUID actorUserId, UUID groupId) {
        Long count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM attendance_sessions s
                LEFT JOIN session_attendance sa
                  ON sa.session_id = s.id
                 AND sa.user_id = UUID_TO_BIN(?, 1)
                WHERE s.group_id = UUID_TO_BIN(?, 1)
                  AND s.deleted_at IS NULL
                  AND s.status <> 'CANCELLED'
                  AND s.start_at <= CURRENT_TIMESTAMP
                """,
                Long.class,
                actorUserId.toString(),
                groupId.toString()
        );

        return count == null ? 0L : count;
    }

    public List<MyAttendanceHistoryItemResponse> findGroupAttendanceForExport(UUID groupId) {
        return jdbcTemplate.query(
                """
                SELECT
                    BIN_TO_UUID(s.id, 1) AS session_id,
                    BIN_TO_UUID(g.id, 1) AS group_id,
                    g.name AS group_name,
                    COALESCE(s.title, CONCAT('Buổi học ', DATE_FORMAT(s.session_date, '%d/%m/%Y'))) AS session_name,
                    s.session_date,
                    s.start_at,
                    s.end_at,
                    s.status AS session_status,
                    COALESCE(sa.attendance_status, 'ABSENT') AS attendance_status,
                    sa.check_in_at,
                    sa.check_in_method,
                    COALESCE(sa.suspicious_flag, false) AS suspicious_flag,
                    sa.suspicious_reason
                FROM attendance_sessions s
                JOIN class_groups g
                  ON g.id = s.group_id
                 AND g.deleted_at IS NULL
                JOIN group_members gm
                  ON gm.group_id = g.id
                 AND gm.member_status = 'APPROVED'
                LEFT JOIN session_attendance sa
                  ON sa.session_id = s.id
                 AND sa.user_id = gm.user_id
                WHERE s.group_id = UUID_TO_BIN(?, 1)
                  AND s.deleted_at IS NULL
                  AND s.status <> 'CANCELLED'
                ORDER BY s.session_date ASC, s.start_at ASC, gm.joined_at ASC
                """,
                (rs, rowNum) -> new MyAttendanceHistoryItemResponse(
                        UUID.fromString(rs.getString("session_id")),
                        UUID.fromString(rs.getString("group_id")),
                        rs.getString("group_name"),
                        rs.getString("session_name"),
                        toLocalDate(rs.getObject("session_date")),
                        toInstant(rs.getObject("start_at")),
                        toInstant(rs.getObject("end_at")),
                        rs.getString("session_status"),
                        rs.getString("attendance_status"),
                        toInstant(rs.getObject("check_in_at")),
                        rs.getString("check_in_method"),
                        rs.getBoolean("suspicious_flag"),
                        rs.getString("suspicious_reason")
                ),
                groupId.toString()
        );
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
            // MySQL/JDBC can return DATETIME as a local datetime string without zone,
            // for example: 2026-04-26T05:49:25.099
            return LocalDateTime.parse(text)
                    .atZone(ZoneId.systemDefault())
                    .toInstant();
        }
    }

    private LocalDate toLocalDate(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof LocalDate localDate) {
            return localDate;
        }

        if (value instanceof Date date) {
            return date.toLocalDate();
        }

        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }

        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
        }

        return LocalDate.parse(value.toString());
    }
}