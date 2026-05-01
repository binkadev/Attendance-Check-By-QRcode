package com.attendance.backend.me.repository.impl;

import com.attendance.backend.common.persistence.UuidBinary16SwapConverter;
import com.attendance.backend.me.api.response.MyClassTimelineItemResponse;
import com.attendance.backend.me.repository.MyClassTimelineRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class MyClassTimelineRepositoryImpl implements MyClassTimelineRepository {

    private static final UuidBinary16SwapConverter UUID_CONVERTER = new UuidBinary16SwapConverter();

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public MyClassTimelineRepositoryImpl(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<MyClassTimelineItemResponse> findMyClassTimeline(
            UUID actorUserId,
            LocalDate today,
            LocalDate tomorrow,
            LocalDateTime now
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("actorUserId", uuidToBytes(actorUserId));
        params.addValue("today", Date.valueOf(today));
        params.addValue("tomorrow", Date.valueOf(tomorrow));
        params.addValue("now", Timestamp.valueOf(now));

        String sql =
                "WITH target_dates AS ( " +
                        "    SELECT :today AS occurrence_date " +
                        "    UNION ALL " +
                        "    SELECT :tomorrow AS occurrence_date " +
                        "), visible_schedules AS ( " +
                        "    SELECT " +
                        "        cg.id AS group_id, " +
                        "        cg.name AS group_name, " +
                        "        cg.course_code AS course_code, " +
                        "        cg.class_code AS class_code, " +
                        "        cg.room AS room, " +
                        "        cg.campus AS campus, " +
                        "        cg.semester AS semester, " +
                        "        cg.academic_year AS academic_year, " +
                        "        cg.thumbnail_url AS thumbnail_url, " +
                        "        gm.role AS my_role, " +
                        "        gm.member_status AS my_member_status, " +
                        "        lecturer.full_name AS lecturer_name, " +
                        "        lecturer.avatar_url AS avatar_url, " +
                        "        CASE " +
                        "            WHEN gm.role IN ('OWNER', 'CO_HOST') THEN 'Giảng viên' " +
                        "            WHEN gm.role = 'MEMBER' THEN 'Sinh viên' " +
                        "            ELSE gm.role " +
                        "        END AS role_label, " +
                        "        d.occurrence_date AS occurrence_date, " +
                        "        gws.day_of_week AS day_of_week, " +
                        "        TIMESTAMP(d.occurrence_date, gws.start_time) AS start_at, " +
                        "        TIMESTAMP(d.occurrence_date, gws.end_time) AS end_at " +
                        "    FROM group_members gm " +
                        "    INNER JOIN class_groups cg ON cg.id = gm.group_id AND cg.deleted_at IS NULL " +
                        "    INNER JOIN users lecturer ON lecturer.id = cg.owner_user_id AND lecturer.deleted_at IS NULL " +
                        "    INNER JOIN group_weekly_schedules gws ON gws.group_id = cg.id AND gws.deleted_at IS NULL " +
                        "    INNER JOIN target_dates d ON UPPER(DAYNAME(d.occurrence_date)) = gws.day_of_week " +
                        "    WHERE gm.user_id = :actorUserId " +
                        "      AND gm.member_status = 'APPROVED' " +
                        "      AND cg.status = 'ACTIVE' " +
                        "      AND cg.start_date IS NOT NULL " +
                        "      AND cg.planned_end_date IS NOT NULL " +
                        "      AND d.occurrence_date BETWEEN cg.start_date AND cg.planned_end_date " +
                        "), approved_counts AS ( " +
                        "    SELECT " +
                        "        gm.group_id AS group_id, " +
                        "        COUNT(*) AS approved_student_count " +
                        "    FROM group_members gm " +
                        "    WHERE gm.member_status = 'APPROVED' " +
                        "      AND gm.role = 'MEMBER' " +
                        "    GROUP BY gm.group_id " +
                        "), ranked_sessions AS ( " +
                        "    SELECT " +
                        "        s.id AS session_id, " +
                        "        s.group_id AS group_id, " +
                        "        s.session_date AS session_date, " +
                        "        s.status AS session_status, " +
                        "        s.checkin_open_at AS checkin_open_at, " +
                        "        s.checkin_close_at AS checkin_close_at, " +
                        "        ROW_NUMBER() OVER ( " +
                        "            PARTITION BY s.group_id, s.session_date " +
                        "            ORDER BY " +
                        "                CASE WHEN s.status = 'OPEN' THEN 0 ELSE 1 END ASC, " +
                        "                s.start_at ASC, " +
                        "                s.created_at DESC, " +
                        "                s.id DESC " +
                        "        ) AS rn " +
                        "    FROM attendance_sessions s " +
                        "    INNER JOIN visible_schedules vs ON vs.group_id = s.group_id AND vs.occurrence_date = s.session_date " +
                        "    WHERE s.deleted_at IS NULL " +
                        "      AND s.status <> 'CANCELLED' " +
                        ") " +
                        "SELECT " +
                        "    vs.group_id AS group_id, " +
                        "    vs.group_name AS group_name, " +
                        "    vs.role_label AS role_label, " +
                        "    vs.course_code AS course_code, " +
                        "    vs.class_code AS class_code, " +
                        "    vs.room AS room, " +
                        "    vs.campus AS campus, " +
                        "    CASE " +
                        "        WHEN NULLIF(TRIM(COALESCE(vs.campus, '')), '') IS NOT NULL " +
                        "         AND NULLIF(TRIM(COALESCE(vs.room, '')), '') IS NOT NULL " +
                        "            THEN CONCAT(vs.campus, ' - ', vs.room) " +
                        "        WHEN NULLIF(TRIM(COALESCE(vs.campus, '')), '') IS NOT NULL " +
                        "            THEN vs.campus " +
                        "        WHEN NULLIF(TRIM(COALESCE(vs.room, '')), '') IS NOT NULL " +
                        "            THEN vs.room " +
                        "        ELSE NULL " +
                        "    END AS location_display, " +
                        "    vs.lecturer_name AS lecturer_name, " +
                        "    COALESCE(ac.approved_student_count, 0) AS approved_student_count, " +
                        "    vs.semester AS semester, " +
                        "    vs.academic_year AS academic_year, " +
                        "    vs.thumbnail_url AS thumbnail, " +
                        "    vs.avatar_url AS avatar_url, " +
                        "    vs.my_role AS my_role, " +
                        "    vs.my_member_status AS my_member_status, " +
                        "    vs.occurrence_date AS occurrence_date, " +
                        "    vs.day_of_week AS day_of_week, " +
                        "    vs.start_at AS start_at, " +
                        "    vs.end_at AS end_at, " +
                        "    rs.session_id AS representative_session_id, " +
                        "    rs.session_status AS representative_session_status, " +
                        "    rs.checkin_open_at AS checkin_open_at, " +
                        "    rs.checkin_close_at AS checkin_close_at " +
                        "FROM visible_schedules vs " +
                        "LEFT JOIN approved_counts ac ON ac.group_id = vs.group_id " +
                        "LEFT JOIN ranked_sessions rs ON rs.group_id = vs.group_id AND rs.session_date = vs.occurrence_date AND rs.rn = 1 " +
                        "WHERE (vs.occurrence_date = :today AND vs.end_at > :now) " +
                        "   OR vs.occurrence_date = :tomorrow " +
                        "ORDER BY " +
                        "    CASE " +
                        "        WHEN vs.occurrence_date = :today AND vs.start_at <= :now AND vs.end_at > :now THEN 0 " +
                        "        WHEN vs.occurrence_date = :today AND vs.start_at > :now THEN 1 " +
                        "        ELSE 2 " +
                        "    END ASC, " +
                        "    vs.start_at ASC, " +
                        "    LOWER(vs.group_name) ASC, " +
                        "    vs.group_id ASC";

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            LocalDate occurrenceDate = rs.getDate("occurrence_date").toLocalDate();
            LocalDateTime startAt = toLocalDateTime(rs.getTimestamp("start_at"));
            LocalDateTime endAt = toLocalDateTime(rs.getTimestamp("end_at"));

            MyClassTimelineItemResponse item = new MyClassTimelineItemResponse();
            item.setBucket(resolveBucket(occurrenceDate, today, tomorrow, startAt, endAt, now));
            item.setGroupId(bytesToUuid(rs.getBytes("group_id")));
            item.setGroupName(rs.getString("group_name"));
            item.setRoleLabel(rs.getString("role_label"));
            item.setCourseCode(rs.getString("course_code"));
            item.setClassCode(rs.getString("class_code"));
            item.setRoom(rs.getString("room"));
            item.setCampus(rs.getString("campus"));
            item.setLocationDisplay(rs.getString("location_display"));
            item.setLecturerName(rs.getString("lecturer_name"));
            item.setApprovedStudentCount(rs.getLong("approved_student_count"));
            item.setSemester(rs.getString("semester"));
            item.setAcademicYear(rs.getString("academic_year"));
            item.setThumbnail(rs.getString("thumbnail"));
            item.setAvatarUrl(rs.getString("avatar_url"));
            item.setMyRole(rs.getString("my_role"));
            item.setMyMemberStatus(rs.getString("my_member_status"));
            item.setOccurrenceDate(occurrenceDate);
            item.setDayOfWeek(rs.getString("day_of_week"));
            item.setStartAt(startAt);
            item.setEndAt(endAt);
            item.setRepresentativeSessionId(bytesToUuid(rs.getBytes("representative_session_id")));
            item.setRepresentativeSessionStatus(rs.getString("representative_session_status"));
            item.setCheckinOpenAt(toLocalDateTime(rs.getTimestamp("checkin_open_at")));
            item.setCheckinCloseAt(toLocalDateTime(rs.getTimestamp("checkin_close_at")));
            return item;
        });
    }

    private String resolveBucket(
            LocalDate occurrenceDate,
            LocalDate today,
            LocalDate tomorrow,
            LocalDateTime startAt,
            LocalDateTime endAt,
            LocalDateTime now
    ) {
        if (today.equals(occurrenceDate) && (startAt.isEqual(now) || startAt.isBefore(now)) && endAt.isAfter(now)) {
            return "ONGOING";
        }
        if (today.equals(occurrenceDate)) {
            return "UPCOMING_TODAY";
        }
        if (tomorrow.equals(occurrenceDate)) {
            return "UPCOMING_TOMORROW";
        }
        return "UNKNOWN";
    }

    private LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private byte[] uuidToBytes(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        return UUID_CONVERTER.convertToDatabaseColumn(uuid);
    }

    private UUID bytesToUuid(byte[] bytes) {
        if (bytes == null || bytes.length != 16) {
            return null;
        }
        return UUID_CONVERTER.convertToEntityAttribute(bytes);
    }
}
