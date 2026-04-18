package com.attendance.backend.me.repository.impl;

import com.attendance.backend.me.api.response.MyClassResponse;
import com.attendance.backend.me.api.response.MyClassSemesterOptionResponse;
import com.attendance.backend.me.api.response.PageMyClassResponse;
import com.attendance.backend.me.model.MyClassQueryCriteria;
import com.attendance.backend.me.repository.MyClassQueryRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class MyClassQueryRepositoryImpl implements MyClassQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public MyClassQueryRepositoryImpl(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PageMyClassResponse findMyClasses(UUID actorUserId, MyClassQueryCriteria criteria) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("actorUserId", uuidToBytes(actorUserId));

        String baseFromWhere = buildBaseFromWhere(criteria, params);

        String countSql = "SELECT COUNT(*) " + baseFromWhere;
        Long totalElements = jdbcTemplate.queryForObject(countSql, params, Long.class);
        long total = totalElements == null ? 0L : totalElements;

        params.addValue("limit", criteria.getSize());
        params.addValue("offset", (long) criteria.getPage() * criteria.getSize());

        String orderBy = buildOrderBy(criteria);

        String listSql =
                "WITH visible_groups AS ( " +
                        "    SELECT " +
                        "        gm.group_id AS group_id, " +
                        "        gm.role AS my_role, " +
                        "        gm.member_status AS my_member_status, " +
                        "        gm.joined_at AS joined_at, " +
                        "        cg.name AS group_name, " +
                        "        cg.code AS legacy_code, " +
                        "        cg.course_code AS course_code, " +
                        "        cg.class_code AS class_code, " +
                        "        cg.room AS room, " +
                        "        cg.campus AS campus, " +
                        "        cg.semester AS semester, " +
                        "        cg.academic_year AS academic_year, " +
                        "        cg.thumbnail_url AS thumbnail_url, " +
                        "        cg.created_at AS group_created_at, " +
                        "        cg.updated_at AS group_updated_at, " +
                        "        lecturer.full_name AS lecturer_name, " +
                        "        lecturer.avatar_url AS avatar_url, " +
                        "        CASE " +
                        "            WHEN gm.role IN ('OWNER', 'CO_HOST') THEN 'Giảng viên' " +
                        "            WHEN gm.role = 'MEMBER' THEN 'Sinh viên' " +
                        "            ELSE gm.role " +
                        "        END AS role_label " +
                        baseFromWhere +
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
                        "        s.group_id AS group_id, " +
                        "        s.start_at AS start_at, " +
                        "        s.end_at AS end_at, " +
                        "        ROW_NUMBER() OVER ( " +
                        "            PARTITION BY s.group_id " +
                        "            ORDER BY " +
                        "                CASE " +
                        "                    WHEN s.status = 'OPEN' THEN 0 " +
                        "                    WHEN s.start_at >= CURRENT_TIMESTAMP(3) THEN 1 " +
                        "                    ELSE 2 " +
                        "                END ASC, " +
                        "                CASE WHEN s.status = 'OPEN' THEN s.start_at END ASC, " +
                        "                CASE WHEN s.status <> 'OPEN' AND s.start_at >= CURRENT_TIMESTAMP(3) THEN s.start_at END ASC, " +
                        "                CASE WHEN s.status <> 'OPEN' AND s.start_at < CURRENT_TIMESTAMP(3) THEN s.start_at END DESC, " +
                        "                s.created_at DESC, " +
                        "                s.id DESC " +
                        "        ) AS rn " +
                        "    FROM attendance_sessions s " +
                        "    INNER JOIN visible_groups vg ON vg.group_id = s.group_id " +
                        "    WHERE s.deleted_at IS NULL " +
                        "      AND s.status <> 'CANCELLED' " +
                        ") " +
                        "SELECT " +
                        "    vg.group_id AS group_id, " +
                        "    vg.group_name AS group_name, " +
                        "    vg.role_label AS role_label, " +
                        "    vg.course_code AS course_code, " +
                        "    vg.class_code AS class_code, " +
                        "    rs.start_at AS start_time, " +
                        "    rs.end_at AS end_time, " +
                        "    vg.room AS room, " +
                        "    vg.campus AS campus, " +
                        "    CASE " +
                        "        WHEN NULLIF(TRIM(COALESCE(vg.campus, '')), '') IS NOT NULL " +
                        "         AND NULLIF(TRIM(COALESCE(vg.room, '')), '') IS NOT NULL " +
                        "            THEN CONCAT(vg.campus, ' - ', vg.room) " +
                        "        WHEN NULLIF(TRIM(COALESCE(vg.campus, '')), '') IS NOT NULL " +
                        "            THEN vg.campus " +
                        "        WHEN NULLIF(TRIM(COALESCE(vg.room, '')), '') IS NOT NULL " +
                        "            THEN vg.room " +
                        "        ELSE NULL " +
                        "    END AS location_display, " +
                        "    vg.lecturer_name AS lecturer_name, " +
                        "    COALESCE(ac.approved_student_count, 0) AS approved_student_count, " +
                        "    vg.semester AS semester, " +
                        "    vg.academic_year AS academic_year, " +
                        "    vg.thumbnail_url AS thumbnail, " +
                        "    vg.avatar_url AS avatar_url, " +
                        "    vg.my_role AS my_role, " +
                        "    vg.my_member_status AS my_member_status, " +
                        "    vg.joined_at AS joined_at, " +
                        "    vg.group_created_at AS created_at, " +
                        "    vg.group_updated_at AS updated_at " +
                        "FROM visible_groups vg " +
                        "LEFT JOIN approved_counts ac ON ac.group_id = vg.group_id " +
                        "LEFT JOIN ranked_sessions rs ON rs.group_id = vg.group_id AND rs.rn = 1 " +
                        "ORDER BY " + orderBy + " " +
                        "LIMIT :limit OFFSET :offset";

        List<MyClassResponse> items = jdbcTemplate.query(listSql, params, (rs, rowNum) -> {
            MyClassResponse item = new MyClassResponse();
            item.setGroupId(bytesToUuid(rs.getBytes("group_id")));
            item.setGroupName(rs.getString("group_name"));
            item.setRoleLabel(rs.getString("role_label"));
            item.setCourseCode(rs.getString("course_code"));
            item.setClassCode(rs.getString("class_code"));
            item.setStartTime(toLocalDateTime(rs.getTimestamp("start_time")));
            item.setEndTime(toLocalDateTime(rs.getTimestamp("end_time")));
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
            item.setJoinedAt(toLocalDateTime(rs.getTimestamp("joined_at")));
            item.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
            item.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
            return item;
        });

        PageMyClassResponse response = new PageMyClassResponse();
        response.setItems(items);
        response.setPage(criteria.getPage());
        response.setSize(criteria.getSize());
        response.setTotalElements(total);
        response.setTotalPages(calculateTotalPages(total, criteria.getSize()));
        return response;
    }

    @Override
    public List<MyClassSemesterOptionResponse> findMyClassSemesters(UUID actorUserId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("actorUserId", uuidToBytes(actorUserId));

        String sql =
                "WITH visible_groups AS ( " +
                        "    SELECT DISTINCT " +
                        "        NULLIF(TRIM(cg.semester), '') AS semester, " +
                        "        NULLIF(TRIM(cg.academic_year), '') AS academic_year " +
                        "    FROM group_members gm " +
                        "    INNER JOIN class_groups cg ON cg.id = gm.group_id AND cg.deleted_at IS NULL " +
                        "    WHERE gm.user_id = :actorUserId " +
                        ") " +
                        "SELECT " +
                        "    semester, " +
                        "    academic_year " +
                        "FROM visible_groups " +
                        "WHERE semester IS NOT NULL OR academic_year IS NOT NULL " +
                        "ORDER BY " +
                        "    CASE " +
                        "        WHEN academic_year REGEXP '^[0-9]{4}-[0-9]{4}$' THEN CAST(SUBSTRING_INDEX(academic_year, '-', 1) AS UNSIGNED) " +
                        "        ELSE 0 " +
                        "    END DESC, " +
                        "    CASE " +
                        "        WHEN semester REGEXP '[0-9]+' THEN CAST(REGEXP_SUBSTR(semester, '[0-9]+') AS UNSIGNED) " +
                        "        ELSE -1 " +
                        "    END DESC, " +
                        "    academic_year DESC, " +
                        "    semester DESC";

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            String semester = rs.getString("semester");
            String academicYear = rs.getString("academic_year");

            MyClassSemesterOptionResponse item = new MyClassSemesterOptionResponse();
            item.setSemester(semester);
            item.setAcademicYear(academicYear);
            item.setLabel(buildSemesterLabel(semester, academicYear));
            return item;
        });
    }

    private String buildBaseFromWhere(MyClassQueryCriteria criteria, MapSqlParameterSource params) {
        StringBuilder sql = new StringBuilder();

        sql.append(" FROM group_members gm ");
        sql.append(" INNER JOIN class_groups cg ON cg.id = gm.group_id AND cg.deleted_at IS NULL ");
        sql.append(" INNER JOIN users lecturer ON lecturer.id = cg.owner_user_id AND lecturer.deleted_at IS NULL ");
        sql.append(" WHERE gm.user_id = :actorUserId ");

        switch (criteria.getScope()) {
            case TEACHING -> sql.append(" AND gm.role IN ('OWNER', 'CO_HOST') ");
            case STUDYING -> sql.append(" AND gm.role = 'MEMBER' ");
            case ALL -> {
                // no-op
            }
        }

        if (criteria.getStatus() != null) {
            sql.append(" AND cg.status = :status ");
            params.addValue("status", criteria.getStatus());
        }

        if (criteria.getMemberStatus() != null) {
            sql.append(" AND gm.member_status = :memberStatus ");
            params.addValue("memberStatus", criteria.getMemberStatus());
        }

        if (criteria.getSemester() != null) {
            sql.append(" AND LOWER(TRIM(cg.semester)) = :semester ");
            params.addValue("semester", criteria.getSemester().toLowerCase());
        }

        if (criteria.getAcademicYear() != null) {
            sql.append(" AND LOWER(TRIM(cg.academic_year)) = :academicYear ");
            params.addValue("academicYear", criteria.getAcademicYear().toLowerCase());
        }

        if (criteria.getQ() != null) {
            sql.append(" AND ( ");
            sql.append("     LOWER(cg.name) LIKE :qLike ");
            sql.append("  OR LOWER(cg.code) LIKE :qLike ");
            sql.append("  OR LOWER(COALESCE(cg.course_code, '')) LIKE :qLike ");
            sql.append("  OR LOWER(COALESCE(cg.class_code, '')) LIKE :qLike ");
            sql.append("  OR LOWER(lecturer.full_name) LIKE :qLike ");
            sql.append(" ) ");
            params.addValue("qLike", "%" + criteria.getQ().toLowerCase() + "%");
        }

        return sql.toString();
    }

    private String buildOrderBy(MyClassQueryCriteria criteria) {
        String direction = criteria.getSortDir().name();

        return switch (criteria.getSortBy()) {
            case UPDATED_AT ->
                    "vg.group_updated_at " + direction + ", vg.group_created_at DESC, vg.group_id ASC";
            case CREATED_AT ->
                    "vg.group_created_at " + direction + ", vg.group_updated_at DESC, vg.group_id ASC";
            case NAME ->
                    "LOWER(vg.group_name) " + direction + ", vg.group_updated_at DESC, vg.group_id ASC";
        };
    }

    private int calculateTotalPages(long totalElements, int size) {
        if (size <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalElements / (double) size);
    }

    private String buildSemesterLabel(String semester, String academicYear) {
        boolean hasSemester = semester != null && !semester.isBlank();
        boolean hasAcademicYear = academicYear != null && !academicYear.isBlank();

        if (hasSemester && hasAcademicYear) {
            return semester + ", " + academicYear;
        }
        if (hasSemester) {
            return semester;
        }
        return academicYear;
    }

    private LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private byte[] uuidToBytes(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }

    private UUID bytesToUuid(byte[] bytes) {
        if (bytes == null || bytes.length != 16) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        long high = buffer.getLong();
        long low = buffer.getLong();
        return new UUID(high, low);
    }
}