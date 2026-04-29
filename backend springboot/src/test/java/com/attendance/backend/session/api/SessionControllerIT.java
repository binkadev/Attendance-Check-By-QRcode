package com.attendance.backend.session.api;

import com.attendance.backend.security.UserPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "attendance.session.auto-close.initial-delay-ms=3600000",
        "attendance.session.auto-close.fixed-delay-ms=3600000"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SessionControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        cleanupTables();
    }

    @Test
    void create_session_success_owner() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        insertUser(ownerUserId, "owner-session@example.com", "Owner Session");
        insertGroup(groupId, ownerUserId, "Lập trình Android", "SES001", "ACTIVE");
        insertMember(groupId, ownerUserId, "OWNER", "APPROVED");

        String requestBody = """
                {
                  "title": "Buổi học số 1",
                  "startAt": "2099-04-21T01:00:00Z",
                  "endAt": "2099-04-21T03:00:00Z",
                  "checkinOpenAt": "2099-04-21T01:00:00Z",
                  "checkinCloseAt": "2099-04-21T01:20:00Z",
                  "timeWindowMinutes": 20,
                  "lateAfterMinutes": 10,
                  "qrRotateSeconds": 30,
                  "allowManualOverride": true,
                  "note": "Ghi chú phiên học"
                }
                """;

        String responseBody = mockMvc.perform(post("/api/v1/groups/{groupId}/sessions", groupId)
                        .with(auth(ownerUserId))
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.groupId", is(groupId.toString())))
                .andExpect(jsonPath("$.createdByUserId", is(ownerUserId.toString())))
                .andExpect(jsonPath("$.title", is("Buổi học số 1")))
                .andExpect(jsonPath("$.sessionDate", is("2099-04-21")))
                .andExpect(jsonPath("$.status", is("OPEN")))
                .andExpect(jsonPath("$.startAt", is("2099-04-21T01:00:00Z")))
                .andExpect(jsonPath("$.endAt", is("2099-04-21T03:00:00Z")))
                .andExpect(jsonPath("$.checkinOpenAt", is("2099-04-21T01:00:00Z")))
                .andExpect(jsonPath("$.checkinCloseAt", is("2099-04-21T01:20:00Z")))
                .andExpect(jsonPath("$.timeWindowMinutes", is(20)))
                .andExpect(jsonPath("$.lateAfterMinutes", is(10)))
                .andExpect(jsonPath("$.qrRotateSeconds", is(30)))
                .andExpect(jsonPath("$.allowManualOverride", is(true)))
                .andExpect(jsonPath("$.note", is("Ghi chú phiên học")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(responseBody);
        String sessionId = json.get("id").asText();
        assertNotNull(sessionId);

        Map<String, Object> sessionRow = jdbcTemplate.queryForMap(
                """
                SELECT
                    BIN_TO_UUID(id, 1) AS id,
                    BIN_TO_UUID(group_id, 1) AS group_id,
                    BIN_TO_UUID(created_by_user_id, 1) AS created_by_user_id,
                    title,
                    session_date,
                    status,
                    start_at,
                    end_at,
                    checkin_open_at,
                    checkin_close_at,
                    time_window_minutes,
                    late_after_minutes,
                    qr_rotate_seconds,
                    allow_manual_override,
                    note,
                    session_secret
                FROM attendance_sessions
                WHERE id = UUID_TO_BIN(?, 1)
                """,
                sessionId
        );

        assertEquals(sessionId, sessionRow.get("id"));
        assertEquals(groupId.toString(), sessionRow.get("group_id"));
        assertEquals(ownerUserId.toString(), sessionRow.get("created_by_user_id"));
        assertEquals("Buổi học số 1", sessionRow.get("title"));
        assertEquals("OPEN", sessionRow.get("status"));
        assertEquals(20, ((Number) sessionRow.get("time_window_minutes")).intValue());
        assertEquals(10, ((Number) sessionRow.get("late_after_minutes")).intValue());
        assertEquals(30, ((Number) sessionRow.get("qr_rotate_seconds")).intValue());
        assertEquals(1, ((Number) sessionRow.get("allow_manual_override")).intValue());
        assertEquals("Ghi chú phiên học", sessionRow.get("note"));
        assertNotNull(sessionRow.get("session_secret"));
    }

    @Test
    void create_session_unauthorized_returns_401() throws Exception {
        UUID groupId = UUID.randomUUID();

        String requestBody = """
                {
                  "title": "Buổi học số 1",
                  "startAt": "2099-04-21T01:00:00Z"
                }
                """;

        mockMvc.perform(post("/api/v1/groups/{groupId}/sessions", groupId)
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_session_forbidden_member_returns_403() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID memberUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        insertUser(ownerUserId, "owner-session-forbidden@example.com", "Owner Session Forbidden");
        insertUser(memberUserId, "member-session-forbidden@example.com", "Member Session Forbidden");
        insertGroup(groupId, ownerUserId, "Mạng máy tính", "SES002", "ACTIVE");
        insertMember(groupId, ownerUserId, "OWNER", "APPROVED");
        insertMember(groupId, memberUserId, "MEMBER", "APPROVED");

        String requestBody = """
                {
                  "title": "Buổi học số 1",
                  "startAt": "2099-04-21T01:00:00Z"
                }
                """;

        mockMvc.perform(post("/api/v1/groups/{groupId}/sessions", groupId)
                        .with(auth(memberUserId))
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("FORBIDDEN")));
    }

    @Test
    void create_session_conflict_when_group_already_has_open_session() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID openSessionId = UUID.randomUUID();

        insertUser(ownerUserId, "owner-session-conflict@example.com", "Owner Session Conflict");
        insertGroup(groupId, ownerUserId, "Cơ sở dữ liệu", "SES003", "ACTIVE");
        insertMember(groupId, ownerUserId, "OWNER", "APPROVED");

        Instant startAt = Instant.now().plusSeconds(3600);
        insertSession(openSessionId, groupId, ownerUserId, "Phiên đang mở", "OPEN", startAt, null);

        String requestBody = """
                {
                  "title": "Buổi học mới",
                  "startAt": "2099-04-22T01:00:00Z"
                }
                """;

        mockMvc.perform(post("/api/v1/groups/{groupId}/sessions", groupId)
                        .with(auth(ownerUserId))
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("SESSION_ALREADY_OPEN")));
    }

    @Test
    void create_session_should_auto_close_expired_open_session_then_create_new_one() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID expiredSessionId = UUID.randomUUID();

        insertUser(ownerUserId, "owner-session-expired-create@example.com", "Owner Session Expired Create");
        insertGroup(groupId, ownerUserId, "Cấu trúc dữ liệu", "SES099", "ACTIVE");
        insertMember(groupId, ownerUserId, "OWNER", "APPROVED");

        Instant expiredStartAt = Instant.now().minusSeconds(3600);
        insertSession(expiredSessionId, groupId, ownerUserId, "Phiên cũ đã hết hạn", "OPEN", expiredStartAt, null);

        String requestBody = """
                {
                  "title": "Buổi học mới sau khi phiên cũ hết hạn",
                  "startAt": "2099-04-22T01:00:00Z"
                }
                """;

        mockMvc.perform(post("/api/v1/groups/{groupId}/sessions", groupId)
                        .with(auth(ownerUserId))
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("OPEN")))
                .andExpect(jsonPath("$.title", is("Buổi học mới sau khi phiên cũ hết hạn")));

        Map<String, Object> oldSession = jdbcTemplate.queryForMap(
                """
                SELECT status, end_at
                FROM attendance_sessions
                WHERE id = UUID_TO_BIN(?, 1)
                """,
                expiredSessionId.toString()
        );

        assertEquals("CLOSED", oldSession.get("status"));
        assertNotNull(oldSession.get("end_at"));
    }

    @Test
    void create_session_conflict_when_group_archived() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        insertUser(ownerUserId, "owner-session-archived@example.com", "Owner Session Archived");
        insertGroup(groupId, ownerUserId, "Nhập môn lập trình", "SES004", "ARCHIVED");
        insertMember(groupId, ownerUserId, "OWNER", "APPROVED");

        String requestBody = """
                {
                  "title": "Buổi học số 1",
                  "startAt": "2099-04-21T01:00:00Z"
                }
                """;

        mockMvc.perform(post("/api/v1/groups/{groupId}/sessions", groupId)
                        .with(auth(ownerUserId))
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("GROUP_ARCHIVED")));
    }

    @Test
    void get_open_session_success() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        insertUser(ownerUserId, "owner-open-session@example.com", "Owner Open Session");
        insertGroup(groupId, ownerUserId, "Kiến trúc máy tính", "SES005", "ACTIVE");
        insertMember(groupId, ownerUserId, "OWNER", "APPROVED");

        Instant startAt = Instant.now().plusSeconds(3600);
        insertSession(sessionId, groupId, ownerUserId, "Phiên đang mở", "OPEN", startAt, null);

        mockMvc.perform(get("/api/v1/groups/{groupId}/sessions/open", groupId)
                        .with(auth(ownerUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(sessionId.toString())))
                .andExpect(jsonPath("$.groupId", is(groupId.toString())))
                .andExpect(jsonPath("$.status", is("OPEN")))
                .andExpect(jsonPath("$.title", is("Phiên đang mở")));
    }

    @Test
    void get_open_session_not_found_returns_404() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        insertUser(ownerUserId, "owner-open-not-found@example.com", "Owner Open Not Found");
        insertGroup(groupId, ownerUserId, "Hệ điều hành", "SES006", "ACTIVE");
        insertMember(groupId, ownerUserId, "OWNER", "APPROVED");

        mockMvc.perform(get("/api/v1/groups/{groupId}/sessions/open", groupId)
                        .with(auth(ownerUserId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("SESSION_OPEN_NOT_FOUND")));
    }

    @Test
    void get_open_session_should_auto_close_expired_open_session_and_return_404() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        insertUser(ownerUserId, "owner-expired-open@example.com", "Owner Expired Open");
        insertGroup(groupId, ownerUserId, "Phiên hết hạn", "EXP999", "ACTIVE");
        insertMember(groupId, ownerUserId, "OWNER", "APPROVED");

        Instant startAt = Instant.now().minusSeconds(3600);
        insertSession(sessionId, groupId, ownerUserId, "Phiên đã hết hạn", "OPEN", startAt, null);

        mockMvc.perform(get("/api/v1/groups/{groupId}/sessions/open", groupId)
                        .with(auth(ownerUserId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("SESSION_OPEN_NOT_FOUND")));

        Map<String, Object> row = jdbcTemplate.queryForMap(
                """
                SELECT status, end_at
                FROM attendance_sessions
                WHERE id = UUID_TO_BIN(?, 1)
                """,
                sessionId.toString()
        );

        assertEquals("CLOSED", row.get("status"));
        assertNotNull(row.get("end_at"));
    }

    @Test
    void list_sessions_success_with_paging_and_status_filter() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID closedSessionId = UUID.randomUUID();
        UUID cancelledSessionId = UUID.randomUUID();

        insertUser(ownerUserId, "owner-list-session@example.com", "Owner List Session");
        insertGroup(groupId, ownerUserId, "Lập trình Web", "SES007", "ACTIVE");
        insertMember(groupId, ownerUserId, "OWNER", "APPROVED");

        Instant closedStartAt = Instant.now().minusSeconds(7200);
        Instant cancelledStartAt = Instant.now().minusSeconds(3600);

        insertSession(closedSessionId, groupId, ownerUserId, "Phiên đã đóng", "CLOSED", closedStartAt, closedStartAt.plusSeconds(5400));
        insertSession(cancelledSessionId, groupId, ownerUserId, "Phiên đã hủy", "CANCELLED", cancelledStartAt, cancelledStartAt.plusSeconds(5400));

        mockMvc.perform(get("/api/v1/groups/{groupId}/sessions", groupId)
                        .with(auth(ownerUserId))
                        .param("status", "CLOSED")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].id", is(closedSessionId.toString())))
                .andExpect(jsonPath("$.items[0].status", is("CLOSED")))
                .andExpect(jsonPath("$.items[0].title", is("Phiên đã đóng")))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.totalPages", is(1)));
    }

    @Test
    void list_sessions_should_auto_close_expired_open_session_and_return_closed_when_filter_closed() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID expiredSessionId = UUID.randomUUID();

        insertUser(ownerUserId, "owner-list-expired@example.com", "Owner List Expired");
        insertGroup(groupId, ownerUserId, "Lịch sử phiên hết hạn", "EXP998", "ACTIVE");
        insertMember(groupId, ownerUserId, "OWNER", "APPROVED");

        Instant expiredStartAt = Instant.now().minusSeconds(3600);
        insertSession(expiredSessionId, groupId, ownerUserId, "Phiên hết hạn trong list", "OPEN", expiredStartAt, null);

        mockMvc.perform(get("/api/v1/groups/{groupId}/sessions", groupId)
                        .with(auth(ownerUserId))
                        .param("status", "CLOSED")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].id", is(expiredSessionId.toString())))
                .andExpect(jsonPath("$.items[0].status", is("CLOSED")))
                .andExpect(jsonPath("$.items[0].title", is("Phiên hết hạn trong list")));
    }

    @Test
    void get_session_detail_success() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        insertUser(ownerUserId, "owner-detail-session@example.com", "Owner Detail Session");
        insertGroup(groupId, ownerUserId, "Phân tích thiết kế", "SES008", "ACTIVE");
        insertMember(groupId, ownerUserId, "OWNER", "APPROVED");

        Instant startAt = Instant.now().plusSeconds(3600);
        insertSession(sessionId, groupId, ownerUserId, "Phiên chi tiết", "OPEN", startAt, null);

        mockMvc.perform(get("/api/v1/sessions/{sessionId}", sessionId)
                        .with(auth(ownerUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(sessionId.toString())))
                .andExpect(jsonPath("$.groupId", is(groupId.toString())))
                .andExpect(jsonPath("$.createdByUserId", is(ownerUserId.toString())))
                .andExpect(jsonPath("$.title", is("Phiên chi tiết")))
                .andExpect(jsonPath("$.status", is("OPEN")));
    }

    @Test
    void get_session_detail_should_auto_close_expired_open_session() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        insertUser(ownerUserId, "owner-detail-expired@example.com", "Owner Detail Expired");
        insertGroup(groupId, ownerUserId, "Chi tiết phiên hết hạn", "EXP997", "ACTIVE");
        insertMember(groupId, ownerUserId, "OWNER", "APPROVED");

        Instant expiredStartAt = Instant.now().minusSeconds(3600);
        insertSession(sessionId, groupId, ownerUserId, "Phiên detail đã hết hạn", "OPEN", expiredStartAt, null);

        mockMvc.perform(get("/api/v1/sessions/{sessionId}", sessionId)
                        .with(auth(ownerUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(sessionId.toString())))
                .andExpect(jsonPath("$.status", is("CLOSED")))
                .andExpect(jsonPath("$.title", is("Phiên detail đã hết hạn")));
    }

    @Test
    void close_session_success() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        insertUser(ownerUserId, "owner-close-session@example.com", "Owner Close Session");
        insertGroup(groupId, ownerUserId, "An toàn thông tin", "SES009", "ACTIVE");
        insertMember(groupId, ownerUserId, "OWNER", "APPROVED");

        Instant startAt = Instant.now().plusSeconds(3600);
        insertSession(sessionId, groupId, ownerUserId, "Phiên cần đóng", "OPEN", startAt, null);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/close", sessionId)
                        .with(auth(ownerUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(sessionId.toString())))
                .andExpect(jsonPath("$.status", is("CLOSED")));

        Map<String, Object> row = jdbcTemplate.queryForMap(
                """
                SELECT status, end_at
                FROM attendance_sessions
                WHERE id = UUID_TO_BIN(?, 1)
                """,
                sessionId.toString()
        );

        assertEquals("CLOSED", row.get("status"));
        assertNotNull(row.get("end_at"));
    }

    @Test
    void close_session_forbidden_member_returns_403() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID memberUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        insertUser(ownerUserId, "owner-close-forbidden@example.com", "Owner Close Forbidden");
        insertUser(memberUserId, "member-close-forbidden@example.com", "Member Close Forbidden");
        insertGroup(groupId, ownerUserId, "Trí tuệ nhân tạo", "SES010", "ACTIVE");
        insertMember(groupId, ownerUserId, "OWNER", "APPROVED");
        insertMember(groupId, memberUserId, "MEMBER", "APPROVED");

        Instant startAt = Instant.now().plusSeconds(3600);
        insertSession(sessionId, groupId, ownerUserId, "Phiên không được đóng", "OPEN", startAt, null);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/close", sessionId)
                        .with(auth(memberUserId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("FORBIDDEN")));
    }

    @Test
    void cancel_session_success() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        insertUser(ownerUserId, "owner-cancel-session@example.com", "Owner Cancel Session");
        insertGroup(groupId, ownerUserId, "Điện toán đám mây", "SES011", "ACTIVE");
        insertMember(groupId, ownerUserId, "OWNER", "APPROVED");

        Instant startAt = Instant.now().plusSeconds(3600);
        insertSession(sessionId, groupId, ownerUserId, "Phiên cần hủy", "OPEN", startAt, null);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/cancel", sessionId)
                        .with(auth(ownerUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(sessionId.toString())))
                .andExpect(jsonPath("$.status", is("CANCELLED")));

        Map<String, Object> row = jdbcTemplate.queryForMap(
                """
                SELECT status, end_at
                FROM attendance_sessions
                WHERE id = UUID_TO_BIN(?, 1)
                """,
                sessionId.toString()
        );

        assertEquals("CANCELLED", row.get("status"));
        assertNotNull(row.get("end_at"));
    }

    @Test
    void cancel_expired_open_session_should_auto_close_and_return_conflict() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        insertUser(ownerUserId, "owner-cancel-expired@example.com", "Owner Cancel Expired");
        insertGroup(groupId, ownerUserId, "Hủy phiên hết hạn", "EXP996", "ACTIVE");
        insertMember(groupId, ownerUserId, "OWNER", "APPROVED");

        Instant expiredStartAt = Instant.now().minusSeconds(3600);
        insertSession(sessionId, groupId, ownerUserId, "Phiên hết hạn không được hủy", "OPEN", expiredStartAt, null);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/cancel", sessionId)
                        .with(auth(ownerUserId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("SESSION_ALREADY_CLOSED")));

        Map<String, Object> row = jdbcTemplate.queryForMap(
                """
                SELECT status, end_at
                FROM attendance_sessions
                WHERE id = UUID_TO_BIN(?, 1)
                """,
                sessionId.toString()
        );

        assertEquals("CLOSED", row.get("status"));
        assertNotNull(row.get("end_at"));
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
        LocalDateTime now = LocalDateTime.now().minusDays(1);

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
                Timestamp.valueOf(now),
                Timestamp.valueOf(now),
                null
        );
    }

    private void insertGroup(
            UUID groupId,
            UUID ownerUserId,
            String name,
            String code,
            String status
    ) {
        LocalDateTime now = LocalDateTime.now().minusHours(2);

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
                "Session IT group",
                null,
                "HK2",
                "2025-2026",
                "A101",
                "CS Thu Duc",
                11,
                3,
                "AUTO",
                0,
                status,
                Timestamp.valueOf(now),
                Timestamp.valueOf(now),
                null
        );
    }

    private void insertMember(UUID groupId, UUID userId, String role, String memberStatus) {
        LocalDateTime now = LocalDateTime.now().minusHours(1);

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
                Timestamp.valueOf(now),
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
        Instant checkinCloseAt = startAt.plusSeconds(30 * 60L);
        Instant now = Instant.now().minusSeconds(1800);
        LocalDate sessionDate = LocalDate.ofInstant(startAt, ZoneId.systemDefault());

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
                30,
                5,
                15,
                "test-session-secret-" + sessionId,
                1,
                Timestamp.from(checkinOpenAt),
                Timestamp.from(checkinCloseAt),
                "Session IT note",
                Timestamp.from(now),
                Timestamp.from(now),
                null
        );
    }

    private void cleanupTables() {
        jdbcTemplate.update("DELETE FROM attendance_sessions");
        jdbcTemplate.update("DELETE FROM group_members");
        jdbcTemplate.update("DELETE FROM class_groups");
        jdbcTemplate.update("DELETE FROM users");
    }
}