package com.attendance.backend.attendance.api;

import com.attendance.backend.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AttendanceReadControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        cleanupTables();
    }

    @Test
    void list_upcoming_sessions_success() throws Exception {
        UUID studentUserId = UUID.randomUUID();
        UUID ownerUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        insertUser(studentUserId, "student-upcoming@example.com", "Student Upcoming");
        insertUser(ownerUserId, "owner-upcoming@example.com", "Owner Upcoming");
        insertGroup(groupId, ownerUserId, "Lập trình Android", "UP001", "A101", "ACTIVE");
        insertMember(groupId, studentUserId, "MEMBER", "APPROVED");
        insertMember(groupId, ownerUserId, "OWNER", "APPROVED");

        Instant startAt = Instant.now().plusSeconds(3600);
        Instant endAt = startAt.plusSeconds(7200);

        insertSession(sessionId, groupId, ownerUserId, "Buổi học sắp tới", "OPEN", startAt, endAt);

        mockMvc.perform(get("/api/v1/me/sessions/upcoming")
                        .with(auth(studentUserId))
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].sessionId", is(sessionId.toString())))
                .andExpect(jsonPath("$[0].groupId", is(groupId.toString())))
                .andExpect(jsonPath("$[0].sessionName", is("Buổi học sắp tới")))
                .andExpect(jsonPath("$[0].room", is("A101")))
                .andExpect(jsonPath("$[0].groupName", is("Lập trình Android")));
    }

    @Test
    void list_upcoming_sessions_unauthorized_returns_401() throws Exception {
        mockMvc.perform(get("/api/v1/me/sessions/upcoming"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_my_attendances_in_group_success_absent_when_no_attendance_row() throws Exception {
        UUID studentUserId = UUID.randomUUID();
        UUID ownerUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        insertUser(studentUserId, "student-history@example.com", "Student History");
        insertUser(ownerUserId, "owner-history@example.com", "Owner History");
        insertGroup(groupId, ownerUserId, "Cơ sở dữ liệu", "HIS001", "B201", "ACTIVE");
        insertMember(groupId, studentUserId, "MEMBER", "APPROVED");
        insertMember(groupId, ownerUserId, "OWNER", "APPROVED");

        Instant startAt = Instant.now().minusSeconds(7200);
        Instant endAt = startAt.plusSeconds(5400);

        insertSession(sessionId, groupId, ownerUserId, "Buổi học đã qua", "CLOSED", startAt, endAt);

        mockMvc.perform(get("/api/v1/groups/{groupId}/me/attendances", groupId)
                        .with(auth(studentUserId))
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].sessionId", is(sessionId.toString())))
                .andExpect(jsonPath("$.items[0].groupId", is(groupId.toString())))
                .andExpect(jsonPath("$.items[0].groupName", is("Cơ sở dữ liệu")))
                .andExpect(jsonPath("$.items[0].sessionName", is("Buổi học đã qua")))
                .andExpect(jsonPath("$.items[0].sessionStatus", is("CLOSED")))
                .andExpect(jsonPath("$.items[0].attendanceStatus", is("ABSENT")))
                .andExpect(jsonPath("$.items[0].checkInAt").doesNotExist())
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.totalPages", is(1)));
    }

    @Test
    void list_my_attendances_in_group_forbidden_when_not_member() throws Exception {
        UUID studentUserId = UUID.randomUUID();
        UUID ownerUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        insertUser(studentUserId, "student-not-member@example.com", "Student Not Member");
        insertUser(ownerUserId, "owner-not-member@example.com", "Owner Not Member");
        insertGroup(groupId, ownerUserId, "Mạng máy tính", "HIS002", "C301", "ACTIVE");
        insertMember(groupId, ownerUserId, "OWNER", "APPROVED");

        mockMvc.perform(get("/api/v1/groups/{groupId}/me/attendances", groupId)
                        .with(auth(studentUserId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("FORBIDDEN")));
    }

    @Test
    void export_group_attendance_success_owner() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID studentUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        insertUser(ownerUserId, "owner-export@example.com", "Owner Export");
        insertUser(studentUserId, "student-export@example.com", "Student Export");
        insertGroup(groupId, ownerUserId, "Xuất báo cáo", "EXP001", "D401", "ACTIVE");
        insertMember(groupId, ownerUserId, "OWNER", "APPROVED");
        insertMember(groupId, studentUserId, "MEMBER", "APPROVED");

        Instant startAt = Instant.now().minusSeconds(7200);
        Instant endAt = startAt.plusSeconds(5400);

        insertSession(sessionId, groupId, ownerUserId, "Phiên export", "CLOSED", startAt, endAt);

        byte[] responseBytes = mockMvc.perform(get("/api/v1/groups/{groupId}/attendance/export", groupId)
                        .with(auth(ownerUserId)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", startsWith("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")))
                .andExpect(header().string("Content-Disposition", startsWith("attachment;")))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertTrue(responseBytes.length > 0);
        assertTrue(responseBytes[0] == 'P' && responseBytes[1] == 'K');
    }

    @Test
    void export_group_attendance_forbidden_member_returns_403() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID studentUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        insertUser(ownerUserId, "owner-export-forbidden@example.com", "Owner Export Forbidden");
        insertUser(studentUserId, "student-export-forbidden@example.com", "Student Export Forbidden");
        insertGroup(groupId, ownerUserId, "Export Forbidden", "EXP002", "D402", "ACTIVE");
        insertMember(groupId, ownerUserId, "OWNER", "APPROVED");
        insertMember(groupId, studentUserId, "MEMBER", "APPROVED");

        mockMvc.perform(get("/api/v1/groups/{groupId}/attendance/export", groupId)
                        .with(auth(studentUserId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("FORBIDDEN")));
    }

    @Test
    void export_group_attendance_unauthorized_returns_401() throws Exception {
        UUID groupId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/groups/{groupId}/attendance/export", groupId))
                .andExpect(status().isUnauthorized());
    }

    private RequestPostProcessor auth(UUID userId) {
        UserPrincipal principal = Mockito.mock(UserPrincipal.class);
        when(principal.getUserId()).thenReturn(userId);

        Authentication authToken = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of()
        );

        return SecurityMockMvcRequestPostProcessors.authentication(authToken);
    }

    private void insertUser(UUID id, String email, String fullName) {
        Instant now = Instant.now().minusSeconds(86400);

        jdbcTemplate.update(
                """
                INSERT INTO users (
                    id,
                    platform_role,
                    email,
                    password_hash,
                    full_name,
                    avatar_url,
                    user_code,
                    primary_device_id,
                    status,
                    created_at,
                    updated_at,
                    deleted_at
                ) VALUES (
                    UUID_TO_BIN(?, 1),
                    'USER',
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    'ACTIVE',
                    ?,
                    ?,
                    ?
                )
                """,
                id.toString(),
                email,
                "$2a$10$abcdefghijklmnopqrstuv",
                fullName,
                null,
                null,
                null,
                Timestamp.from(now),
                Timestamp.from(now),
                null
        );
    }

    private void insertGroup(
            UUID groupId,
            UUID ownerUserId,
            String name,
            String code,
            String room,
            String status
    ) {
        Instant now = Instant.now().minusSeconds(7200);

        jdbcTemplate.update(
                """
                INSERT INTO class_groups (
                    id,
                    owner_user_id,
                    name,
                    code,
                    course_code,
                    class_code,
                    join_code,
                    description,
                    thumbnail_url,
                    semester,
                    academic_year,
                    room,
                    campus,
                    total_sessions,
                    max_allowed_absences,
                    approval_mode,
                    allow_auto_join_on_checkin,
                    status,
                    created_at,
                    updated_at,
                    deleted_at
                ) VALUES (
                    UUID_TO_BIN(?, 1),
                    UUID_TO_BIN(?, 1),
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?
                )
                """,
                groupId.toString(),
                ownerUserId.toString(),
                name,
                code,
                "INT1348",
                "D22CQCNPM02-N",
                "JOIN" + code,
                "Attendance read IT group",
                null,
                "HK2",
                "2025-2026",
                room,
                "CS Thu Duc",
                11,
                3,
                "AUTO",
                0,
                status,
                Timestamp.from(now),
                Timestamp.from(now),
                null
        );
    }

    private void insertMember(UUID groupId, UUID userId, String role, String memberStatus) {
        Instant now = Instant.now().minusSeconds(3600);

        jdbcTemplate.update(
                """
                INSERT INTO group_members (
                    group_id,
                    user_id,
                    role,
                    member_status,
                    joined_at,
                    invited_by
                ) VALUES (
                    UUID_TO_BIN(?, 1),
                    UUID_TO_BIN(?, 1),
                    ?,
                    ?,
                    ?,
                    ?
                )
                """,
                groupId.toString(),
                userId.toString(),
                role,
                memberStatus,
                Timestamp.from(now),
                null
        );
    }

    private void insertSession(
            UUID sessionId,
            UUID groupId,
            UUID createdByUserId,
            String title,
            String status,
            Instant startAt,
            Instant endAt
    ) {
        Instant checkinOpenAt = startAt;
        Instant checkinCloseAt = startAt.plusSeconds(15 * 60L);
        Instant now = Instant.now().minusSeconds(1800);
        LocalDate sessionDate = LocalDate.ofInstant(startAt, java.time.ZoneId.systemDefault());

        jdbcTemplate.update(
                """
                INSERT INTO attendance_sessions (
                    id,
                    group_id,
                    created_by_user_id,
                    title,
                    session_date,
                    start_at,
                    end_at,
                    status,
                    time_window_minutes,
                    late_after_minutes,
                    qr_rotate_seconds,
                    session_secret,
                    allow_manual_override,
                    checkin_open_at,
                    checkin_close_at,
                    note,
                    created_at,
                    updated_at,
                    deleted_at
                ) VALUES (
                    UUID_TO_BIN(?, 1),
                    UUID_TO_BIN(?, 1),
                    UUID_TO_BIN(?, 1),
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?
                )
                """,
                sessionId.toString(),
                groupId.toString(),
                createdByUserId.toString(),
                title,
                Date.valueOf(sessionDate),
                Timestamp.from(startAt),
                endAt == null ? null : Timestamp.from(endAt),
                status,
                20,
                10,
                30,
                "test-session-secret-" + sessionId,
                1,
                Timestamp.from(checkinOpenAt),
                Timestamp.from(checkinCloseAt),
                "Attendance read IT note",
                Timestamp.from(now),
                Timestamp.from(now),
                null
        );
    }

    private void cleanupTables() {
        jdbcTemplate.update("DELETE FROM session_attendance");
        jdbcTemplate.update("DELETE FROM attendance_events");
        jdbcTemplate.update("DELETE FROM absence_requests");
        jdbcTemplate.update("DELETE FROM attendance_sessions");
        jdbcTemplate.update("DELETE FROM group_members");
        jdbcTemplate.update("DELETE FROM class_groups");
        jdbcTemplate.update("DELETE FROM users");
    }
}