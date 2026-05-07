package com.attendance.backend.stats.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class AttendanceStatsQueryRepositoryImpl implements AttendanceStatsQueryRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public AttendanceStatsQueryRepositoryImpl(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public StudentSummaryRow findStudentSummary(UUID userId, String semester, String academicYear) {
        String sql = """
                SELECT
                    COUNT(s.id) AS total_sessions,
                    COALESCE(SUM(CASE WHEN COALESCE(sa.attendance_status, 'ABSENT') = 'PRESENT' THEN 1 ELSE 0 END), 0) AS present_count,
                    COALESCE(SUM(CASE WHEN COALESCE(sa.attendance_status, 'ABSENT') = 'LATE' THEN 1 ELSE 0 END), 0) AS late_count,
                    COALESCE(SUM(CASE WHEN COALESCE(sa.attendance_status, 'ABSENT') = 'ABSENT' THEN 1 ELSE 0 END), 0) AS absent_count,
                    COALESCE(SUM(CASE WHEN COALESCE(sa.attendance_status, 'ABSENT') = 'EXCUSED' THEN 1 ELSE 0 END), 0) AS excused_count
                FROM class_groups g
                JOIN group_members gm
                  ON gm.group_id = g.id
                 AND gm.user_id = UUID_TO_BIN(:userId, 1)
                 AND gm.member_status = 'APPROVED'
                 AND gm.role = 'MEMBER'
                JOIN attendance_sessions s
                  ON s.group_id = g.id
                 AND s.deleted_at IS NULL
                 AND s.status = 'CLOSED'
                LEFT JOIN session_attendance sa
                  ON sa.session_id = s.id
                 AND sa.user_id = gm.user_id
                WHERE g.deleted_at IS NULL
                  AND g.status = 'ACTIVE'
                  AND (:semester IS NULL OR g.semester = :semester)
                  AND (:academicYear IS NULL OR g.academic_year = :academicYear)
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId.toString())
                .addValue("semester", blankToNull(semester))
                .addValue("academicYear", blankToNull(academicYear));

        return jdbc.queryForObject(sql, params, studentSummaryRowMapper());
    }

    @Override
    public Page<GroupSummaryRow> findGroupSummaryPage(UUID groupId, int page, int size) {
        String dataSql = """
                SELECT
                    BIN_TO_UUID(gm.user_id, 1) AS user_id,
                    u.full_name AS full_name,
                    u.email AS email,
                    COALESCE(sumx.total_sessions, 0) AS total_sessions,
                    COALESCE(sumx.present_count, 0) AS present_count,
                    COALESCE(sumx.late_count, 0) AS late_count,
                    COALESCE(sumx.absent_count, 0) AS absent_count,
                    COALESCE(sumx.excused_count, 0) AS excused_count
                FROM group_members gm
                JOIN users u
                  ON u.id = gm.user_id
                 AND u.deleted_at IS NULL
                LEFT JOIN (
                    SELECT
                        sa.user_id AS user_id,
                        COUNT(*) AS total_sessions,
                        COALESCE(SUM(CASE WHEN sa.attendance_status = 'PRESENT' THEN 1 ELSE 0 END), 0) AS present_count,
                        COALESCE(SUM(CASE WHEN sa.attendance_status = 'LATE' THEN 1 ELSE 0 END), 0) AS late_count,
                        COALESCE(SUM(CASE WHEN sa.attendance_status = 'ABSENT' THEN 1 ELSE 0 END), 0) AS absent_count,
                        COALESCE(SUM(CASE WHEN sa.attendance_status = 'EXCUSED' THEN 1 ELSE 0 END), 0) AS excused_count
                    FROM session_attendance sa
                    JOIN attendance_sessions s
                      ON s.id = sa.session_id
                    WHERE s.group_id = UUID_TO_BIN(:groupId, 1)
                      AND s.deleted_at IS NULL
                      AND s.status = 'CLOSED'
                    GROUP BY sa.user_id
                ) sumx
                  ON sumx.user_id = gm.user_id
                WHERE gm.group_id = UUID_TO_BIN(:groupId, 1)
                  AND gm.member_status = 'APPROVED'
                ORDER BY u.full_name ASC, u.email ASC, BIN_TO_UUID(gm.user_id, 1) ASC
                LIMIT :limit OFFSET :offset
                """;

        String countSql = """
                SELECT COUNT(*)
                FROM group_members gm
                JOIN users u
                  ON u.id = gm.user_id
                 AND u.deleted_at IS NULL
                WHERE gm.group_id = UUID_TO_BIN(:groupId, 1)
                  AND gm.member_status = 'APPROVED'
                """;

        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        int offset = safePage * safeSize;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("groupId", groupId.toString())
                .addValue("limit", safeSize)
                .addValue("offset", offset);

        List<GroupSummaryRow> items = jdbc.query(dataSql, params, groupSummaryRowMapper());
        Long totalElements = jdbc.queryForObject(countSql, params, Long.class);
        long safeTotalElements = totalElements == null ? 0L : totalElements;

        return new PageImpl<>(
                items,
                PageRequest.of(safePage, safeSize),
                safeTotalElements
        );
    }

    @Override
    public boolean groupExists(UUID groupId) {
        String sql = """
                SELECT COUNT(*)
                FROM class_groups g
                WHERE g.id = UUID_TO_BIN(:groupId, 1)
                  AND g.deleted_at IS NULL
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("groupId", groupId.toString());

        Long count = jdbc.queryForObject(sql, params, Long.class);
        return count != null && count > 0;
    }

    @Override
    public boolean isOwnerOrCoHost(UUID groupId, UUID userId) {
        String sql = """
                SELECT COUNT(*)
                FROM group_members gm
                WHERE gm.group_id = UUID_TO_BIN(:groupId, 1)
                  AND gm.user_id = UUID_TO_BIN(:userId, 1)
                  AND gm.member_status = 'APPROVED'
                  AND gm.role IN ('OWNER', 'CO_HOST')
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("groupId", groupId.toString())
                .addValue("userId", userId.toString());

        Long count = jdbc.queryForObject(sql, params, Long.class);
        return count != null && count > 0;
    }

    private RowMapper<StudentSummaryRow> studentSummaryRowMapper() {
        return (rs, rowNum) -> new StudentSummaryRow(
                rs.getLong("total_sessions"),
                rs.getLong("present_count"),
                rs.getLong("late_count"),
                rs.getLong("absent_count"),
                rs.getLong("excused_count")
        );
    }

    private RowMapper<GroupSummaryRow> groupSummaryRowMapper() {
        return (rs, rowNum) -> new GroupSummaryRow(
                UUID.fromString(rs.getString("user_id")),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getLong("total_sessions"),
                rs.getLong("present_count"),
                rs.getLong("late_count"),
                rs.getLong("absent_count"),
                rs.getLong("excused_count")
        );
    }

    private String blankToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }
}