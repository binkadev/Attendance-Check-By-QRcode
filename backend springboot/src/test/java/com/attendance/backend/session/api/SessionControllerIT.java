package com.attendance.backend.session.api;

import com.attendance.backend.attendance.service.AttendancePolicyNotificationOrchestrator;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    private static final ZoneId NOTIFICATION_BODY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter NOTIFICATION_BODY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter NOTIFICATION_BODY_TIME = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AttendancePolicyNotificationOrchestrator attendancePolicyNotificationOrchestrator;

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
    void close_session_absent_below_warning_should_create_absence_warning_only_and_return_in_me_notifications() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID studentUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        insertUser(ownerUserId, "owner-absence-below@example.com", "Owner Absence Below");
        insertUser(studentUserId, "student-absence-below@example.com", "Student Absence Below");
        insertGroup(groupId, ownerUserId, "Thông báo vắng", "ABW001", "ACTIVE");
        insertMember(groupId, ownerUserId, "OWNER", "APPROVED");
        insertMember(groupId, studentUserId, "MEMBER", "APPROVED");

        Instant baseStartAt = Instant.now().plusSeconds(3600);
        seedClosedHistory(groupId, ownerUserId, studentUserId, baseStartAt, "PRESENT", "PRESENT", "PRESENT", "PRESENT", "PRESENT");
        insertSession(sessionId, groupId, ownerUserId, "Phiên vắng dưới ngưỡng", "OPEN", baseStartAt.plusSeconds(21600), null);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/close", sessionId)
                        .with(auth(ownerUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CLOSED")));

        assertEquals(1L, countNotification(studentUserId, "ABSENCE_WARNING", sessionId));
        assertEquals(0L, countAnyPolicyNotification(studentUserId, sessionId));

        Map<String, Object> row = findNotificationRow(studentUserId, "ABSENCE_WARNING", sessionId);
        assertEquals("WARNING", row.get("severity"));
        assertEquals("ATTENDANCE_SESSION", row.get("source_type"));
        assertEquals("Bạn đã vắng buổi học", row.get("title"));
        assertEquals(sessionId.toString(), row.get("source_ref_id"));

        String body = row.get("body").toString();
        assertContains(body, "Phiên vắng dưới ngưỡng");
        assertContains(body, "môn INT1348");
        assertContains(body, "lớp D22CQCNPM02-N - Thông báo vắng");
        assertContains(body, "Giảng viên: Owner Absence Below");
        assertContains(body, "tại phòng A101, CS Thu Duc");
        assertContains(body, "1/6");
        assertContains(body, "16.67%");
        assertContains(body, "83.33%");
        assertContains(body, "Các buổi vắng được tính");
        assertContains(body, bodyStartAt(baseStartAt.plusSeconds(21600)));
        assertContains(body, "Ngưỡng cảnh báo của lớp là vắng từ 20% hoặc tỷ lệ điểm danh dưới 80%");
        assertDoesNotContain(body, "null");
        assertDoesNotContain(body, "undefined");

        JsonNode payload = notificationPayload(row);
        assertEquals(groupId.toString(), payload.get("groupId").asText());
        assertEquals("Thông báo vắng", payload.get("groupName").asText());
        assertEquals("Thông báo vắng", payload.get("className").asText());
        assertEquals("ABW001", payload.get("groupCode").asText());
        assertEquals("D22CQCNPM02-N", payload.get("classCode").asText());
        assertTrue(payload.get("courseName").isNull());
        assertTrue(payload.get("subjectName").isNull());
        assertEquals("INT1348", payload.get("courseCode").asText());
        assertEquals("INT1348", payload.get("subjectCode").asText());
        assertEquals(sessionId.toString(), payload.get("sessionId").asText());
        assertEquals(sessionId.toString(), payload.get("triggerSessionId").asText());
        assertEquals("Phiên vắng dưới ngưỡng", payload.get("sessionTitle").asText());
        assertEquals("Phiên vắng dưới ngưỡng", payload.get("sessionName").asText());
        assertEquals("Phiên vắng dưới ngưỡng", payload.get("triggerSessionTitle").asText());
        assertEquals("A101", payload.get("triggerSessionRoom").asText());
        assertEquals("A101, CS Thu Duc", payload.get("triggerSessionLocation").asText());
        assertNotNull(payload.get("sessionStartAt").asText(null));
        assertNotNull(payload.get("sessionEndAt").asText(null));
        assertNotNull(payload.get("sessionDate").asText(null));
        assertNotNull(payload.get("computedAt").asText(null));
        assertEquals("A101", payload.get("room").asText());
        assertEquals("A101, CS Thu Duc", payload.get("location").asText());
        assertEquals(ownerUserId.toString(), payload.get("lecturerId").asText());
        assertEquals("Owner Absence Below", payload.get("lecturerName").asText());
        assertEquals("owner-absence-below@example.com", payload.get("lecturerEmail").asText());
        assertEquals("ABSENT", payload.get("attendanceStatus").asText());
        assertEquals(1, payload.get("absentCount").asInt());
        assertEquals(6, payload.get("eligibleSessionCount").asInt());
        assertEquals("16.67", decimalText(payload.get("absenceRate")));
        assertEquals("83.33", decimalText(payload.get("attendanceRate")));
        assertEquals("NORMAL", payload.get("policyStatus").asText());
        assertEquals("NORMAL", payload.get("riskLevel").asText());
        assertEquals("ELIGIBLE", payload.get("examEligibility").asText());
        assertEquals("20", decimalText(payload.get("warningAbsenceRate")));
        assertEquals("25", decimalText(payload.get("criticalAbsenceRate")));
        assertEquals("30", decimalText(payload.get("examBanAbsenceRate")));
        assertEquals(1, payload.get("absenceSessions").size());
        assertEquals(sessionId.toString(), payload.get("absenceSessions").get(0).get("sessionId").asText());
        assertEquals("Phiên vắng dưới ngưỡng", payload.get("absenceSessions").get(0).get("sessionTitle").asText());
        assertEquals("ABSENT", payload.get("absenceSessions").get(0).get("attendanceStatus").asText());
        assertEquals("MISSING_ATTENDANCE_ROW", payload.get("absenceSessions").get(0).get("source").asText());

        mockMvc.perform(get("/api/v1/me/notifications")
                        .with(auth(studentUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].type", hasItem("ABSENCE_WARNING")))
                .andExpect(jsonPath("$.items[?(@.type == 'ABSENCE_WARNING')].title", hasItem("Bạn đã vắng buổi học")));
    }

    @Test
    void close_session_absent_reaching_warning_should_create_absence_and_policy_warning() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID studentUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        insertUser(ownerUserId, "owner-absence-warning@example.com", "Owner Absence Warning");
        insertUser(studentUserId, "student-absence-warning@example.com", "Student Absence Warning");
        insertGroup(groupId, ownerUserId, "Thông báo cảnh báo", "ABW002", "ACTIVE");
        insertMember(groupId, ownerUserId, "OWNER", "APPROVED");
        insertMember(groupId, studentUserId, "MEMBER", "APPROVED");

        Instant baseStartAt = Instant.now().plusSeconds(3600);
        seedClosedHistory(groupId, ownerUserId, studentUserId, baseStartAt, "PRESENT", "PRESENT", "PRESENT", "PRESENT");
        insertSession(sessionId, groupId, ownerUserId, "Phiên chạm cảnh báo", "OPEN", baseStartAt.plusSeconds(21600), null);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/close", sessionId)
                        .with(auth(ownerUserId)))
                .andExpect(status().isOk());

        assertEquals(1L, countNotification(studentUserId, "ABSENCE_WARNING", sessionId));
        assertEquals(1L, countNotification(studentUserId, "ATTENDANCE_POLICY_WARNING", sessionId));
        assertEquals(0L, countNotification(studentUserId, "ATTENDANCE_POLICY_CRITICAL", sessionId));
        assertEquals(0L, countNotification(studentUserId, "ATTENDANCE_POLICY_EXAM_BANNED", sessionId));

        Map<String, Object> policyRow = findNotificationRow(studentUserId, "ATTENDANCE_POLICY_WARNING", sessionId);
        String policyBody = policyRow.get("body").toString();
        assertContains(policyBody, "cảnh báo chuyên cần");
        assertContains(policyBody, "Phiên chạm cảnh báo");
        assertContains(policyBody, "môn INT1348");
        assertContains(policyBody, "lớp D22CQCNPM02-N - Thông báo cảnh báo");
        assertContains(policyBody, "Giảng viên: Owner Absence Warning");
        assertContains(policyBody, "tại phòng A101, CS Thu Duc");
        assertContains(policyBody, "1/5");
        assertContains(policyBody, "20%");
        assertContains(policyBody, "Tỷ lệ điểm danh hiện tại là 80%");
        assertContains(policyBody, "Các buổi vắng được tính");
        assertContains(policyBody, bodyStartAt(baseStartAt.plusSeconds(21600)));
        assertContains(policyBody, "Ngưỡng cảnh báo của lớp là vắng từ 20% hoặc tỷ lệ điểm danh dưới 80%");
        assertDoesNotContain(policyBody, "Tỷ lệ điểm danh của bạn đang ở mức cảnh báo");
        assertDoesNotContain(policyBody, "Tình trạng điểm danh của bạn");
        assertDoesNotContain(policyBody, "null");
        assertDoesNotContain(policyBody, "undefined");

        JsonNode policyPayload = notificationPayload(policyRow);
        assertEquals(groupId.toString(), policyPayload.get("groupId").asText());
        assertEquals("Thông báo cảnh báo", policyPayload.get("className").asText());
        assertEquals("Phiên chạm cảnh báo", policyPayload.get("sessionTitle").asText());
        assertEquals("ABSENT", policyPayload.get("attendanceStatus").asText());
        assertEquals(1, policyPayload.get("absentCount").asInt());
        assertEquals(5, policyPayload.get("eligibleSessionCount").asInt());
        assertEquals("20", decimalText(policyPayload.get("absenceRate")));
        assertEquals("80", decimalText(policyPayload.get("attendanceRate")));
        assertEquals("WARNING", policyPayload.get("policyStatus").asText());
        assertEquals("WARNING", policyPayload.get("riskLevel").asText());
        assertEquals("AT_RISK", policyPayload.get("examEligibility").asText());
        assertNotNull(policyPayload.get("computedAt").asText(null));
        assertEquals("Phiên chạm cảnh báo", policyPayload.get("triggerSessionTitle").asText());
        assertEquals(sessionId.toString(), policyPayload.get("triggerSessionId").asText());
        assertEquals(1, policyPayload.get("absenceSessions").size());
        assertEquals("Phiên chạm cảnh báo", policyPayload.get("absenceSessions").get(0).get("sessionTitle").asText());
        assertEquals("MISSING_ATTENDANCE_ROW", policyPayload.get("absenceSessions").get(0).get("source").asText());
    }

    @Test
    void close_session_absent_reaching_critical_should_include_absence_evidence_and_exclude_excused() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID studentUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        insertUser(ownerUserId, "owner-absence-critical@example.com", "Owner Absence Critical");
        insertUser(studentUserId, "student-absence-critical@example.com", "Student Absence Critical");
        insertGroup(groupId, ownerUserId, "Thông báo nghiêm trọng", "ABW007", "ACTIVE");
        insertMember(groupId, ownerUserId, "OWNER", "APPROVED");
        insertMember(groupId, studentUserId, "MEMBER", "APPROVED");

        Instant baseStartAt = Instant.now().plusSeconds(3600);
        seedClosedHistory(
                groupId,
                ownerUserId,
                studentUserId,
                baseStartAt,
                "PRESENT",
                "PRESENT",
                "PRESENT",
                "PRESENT",
                "PRESENT",
                "ABSENT",
                "EXCUSED"
        );
        insertSession(sessionId, groupId, ownerUserId, "Phiên chạm nghiêm trọng", "OPEN", baseStartAt.plusSeconds(28800), null);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/close", sessionId)
                        .with(auth(ownerUserId)))
                .andExpect(status().isOk());

        assertEquals(1L, countNotification(studentUserId, "ABSENCE_WARNING", sessionId));
        assertEquals(1L, countNotification(studentUserId, "ATTENDANCE_POLICY_CRITICAL", sessionId));
        assertEquals(0L, countNotification(studentUserId, "ATTENDANCE_POLICY_WARNING", sessionId));
        assertEquals(0L, countNotification(studentUserId, "ATTENDANCE_POLICY_EXAM_BANNED", sessionId));

        Map<String, Object> policyRow = findNotificationRow(studentUserId, "ATTENDANCE_POLICY_CRITICAL", sessionId);
        String policyBody = policyRow.get("body").toString();
        assertContains(policyBody, "nguy cơ nghiêm trọng");
        assertContains(policyBody, "môn INT1348");
        assertContains(policyBody, "lớp D22CQCNPM02-N - Thông báo nghiêm trọng");
        assertContains(policyBody, "Giảng viên: Owner Absence Critical");
        assertContains(policyBody, "Phiên chạm nghiêm trọng");
        assertContains(policyBody, "2/7");
        assertContains(policyBody, "28.57%");
        assertContains(policyBody, "71.43%");
        assertContains(policyBody, "Lịch sử điểm danh 6");
        assertContains(policyBody, "Phiên chạm nghiêm trọng");
        assertContains(policyBody, bodyStartAt(baseStartAt.plusSeconds(5 * 3600L)));
        assertContains(policyBody, bodyStartAt(baseStartAt.plusSeconds(28800)));
        assertContains(policyBody, "Ngưỡng nguy cơ nghiêm trọng của lớp là vắng từ 25% hoặc tỷ lệ điểm danh dưới 75%");
        assertDoesNotContain(policyBody, "Lịch sử điểm danh 7");
        assertDoesNotContain(policyBody, "Tình trạng điểm danh của bạn đã ở mức nghiêm trọng.");
        assertDoesNotContain(policyBody, "null");
        assertDoesNotContain(policyBody, "undefined");

        JsonNode policyPayload = notificationPayload(policyRow);
        assertEquals(sessionId.toString(), policyPayload.get("triggerSessionId").asText());
        assertEquals("CRITICAL", policyPayload.get("policyStatus").asText());
        assertEquals("CRITICAL", policyPayload.get("riskLevel").asText());
        assertEquals("AT_RISK", policyPayload.get("examEligibility").asText());
        assertEquals(2, policyPayload.get("absentCount").asInt());
        assertEquals(1, policyPayload.get("excusedCount").asInt());
        assertEquals(7, policyPayload.get("eligibleSessionCount").asInt());
        assertEquals("28.57", decimalText(policyPayload.get("absenceRate")));
        assertEquals("25", decimalText(policyPayload.get("criticalAbsenceRate")));
        JsonNode absenceSessions = policyPayload.get("absenceSessions");
        assertEquals(2, absenceSessions.size());
        assertEquals("Lịch sử điểm danh 6", absenceSessions.get(0).get("sessionTitle").asText());
        assertEquals("SESSION_ATTENDANCE", absenceSessions.get(0).get("source").asText());
        assertEquals("ABSENT", absenceSessions.get(0).get("attendanceStatus").asText());
        assertEquals("Phiên chạm nghiêm trọng", absenceSessions.get(1).get("sessionTitle").asText());
        assertEquals("MISSING_ATTENDANCE_ROW", absenceSessions.get(1).get("source").asText());
        assertEquals("ABSENT", absenceSessions.get(1).get("attendanceStatus").asText());
        assertTrue(!absenceSessionsContainTitle(absenceSessions, "Lịch sử điểm danh 7"));
    }

    @Test
    void close_session_absent_reaching_exam_ban_should_create_absence_and_exam_banned_policy_notification() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID studentUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        insertUser(ownerUserId, "owner-absence-exam@example.com", "Owner Absence Exam");
        insertUser(studentUserId, "student-absence-exam@example.com", "Student Absence Exam");
        insertGroup(groupId, ownerUserId, "Thông báo cấm thi", "ABW003", "ACTIVE");
        insertMember(groupId, ownerUserId, "OWNER", "APPROVED");
        insertMember(groupId, studentUserId, "MEMBER", "APPROVED");

        Instant baseStartAt = Instant.now().plusSeconds(3600);
        seedClosedHistory(
                groupId,
                ownerUserId,
                studentUserId,
                baseStartAt,
                "PRESENT",
                "PRESENT",
                "PRESENT",
                "PRESENT",
                "PRESENT",
                "PRESENT",
                "PRESENT",
                "ABSENT",
                "ABSENT"
        );
        insertSession(sessionId, groupId, ownerUserId, "Phiên chạm cấm thi", "OPEN", baseStartAt.plusSeconds(43200), null);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/close", sessionId)
                        .with(auth(ownerUserId)))
                .andExpect(status().isOk());

        assertEquals(1L, countNotification(studentUserId, "ABSENCE_WARNING", sessionId));
        assertEquals(1L, countNotification(studentUserId, "ATTENDANCE_POLICY_EXAM_BANNED", sessionId));
        assertEquals(0L, countNotification(studentUserId, "ATTENDANCE_POLICY_WARNING", sessionId));
        assertEquals(0L, countNotification(studentUserId, "ATTENDANCE_POLICY_CRITICAL", sessionId));

        Map<String, Object> policyRow = findNotificationRow(studentUserId, "ATTENDANCE_POLICY_EXAM_BANNED", sessionId);
        String policyBody = policyRow.get("body").toString();
        assertContains(policyBody, "không đủ điều kiện dự thi");
        assertContains(policyBody, "Phiên chạm cấm thi");
        assertContains(policyBody, "môn INT1348");
        assertContains(policyBody, "lớp D22CQCNPM02-N - Thông báo cấm thi");
        assertContains(policyBody, "Giảng viên: Owner Absence Exam");
        assertContains(policyBody, "3/10");
        assertContains(policyBody, "30%");
        assertContains(policyBody, "Tỷ lệ điểm danh hiện tại là 70%");
        assertContains(policyBody, "đạt hoặc vượt ngưỡng cấm thi 30%");
        assertContains(policyBody, "Lịch sử điểm danh 8");
        assertContains(policyBody, "Lịch sử điểm danh 9");
        assertContains(policyBody, "Phiên chạm cấm thi");
        assertContains(policyBody, bodyStartAt(baseStartAt.plusSeconds(7 * 3600L)));
        assertContains(policyBody, bodyStartAt(baseStartAt.plusSeconds(8 * 3600L)));
        assertContains(policyBody, "Ngưỡng cấm thi của lớp là vắng từ 30%");
        assertDoesNotContain(policyBody, "Tình trạng điểm danh của bạn");
        assertDoesNotContain(policyBody, "null");
        assertDoesNotContain(policyBody, "undefined");

        JsonNode policyPayload = notificationPayload(policyRow);
        assertEquals("INT1348", policyPayload.get("courseCode").asText());
        assertEquals("D22CQCNPM02-N", policyPayload.get("classCode").asText());
        assertEquals(sessionId.toString(), policyPayload.get("triggerSessionId").asText());
        assertEquals("Phiên chạm cấm thi", policyPayload.get("triggerSessionTitle").asText());
        assertEquals("EXAM_BANNED", policyPayload.get("policyStatus").asText());
        assertEquals("EXAM_BANNED", policyPayload.get("riskLevel").asText());
        assertEquals("BANNED", policyPayload.get("examEligibility").asText());
        assertEquals(3, policyPayload.get("absentCount").asInt());
        assertEquals(10, policyPayload.get("eligibleSessionCount").asInt());
        assertEquals("30", decimalText(policyPayload.get("absenceRate")));
        assertEquals("30", decimalText(policyPayload.get("examBanAbsenceRate")));
        assertTrue(policyPayload.get("examBanBelowRate").isNull());
        assertNotNull(policyPayload.get("computedAt").asText(null));
        JsonNode absenceSessions = policyPayload.get("absenceSessions");
        assertEquals(3, absenceSessions.size());
        assertEquals("Lịch sử điểm danh 8", absenceSessions.get(0).get("sessionTitle").asText());
        assertEquals("SESSION_ATTENDANCE", absenceSessions.get(0).get("source").asText());
        assertEquals("Lịch sử điểm danh 9", absenceSessions.get(1).get("sessionTitle").asText());
        assertEquals("SESSION_ATTENDANCE", absenceSessions.get(1).get("source").asText());
        assertEquals("Phiên chạm cấm thi", absenceSessions.get(2).get("sessionTitle").asText());
        assertEquals("MISSING_ATTENDANCE_ROW", absenceSessions.get(2).get("source").asText());
    }

    @Test
    void reevaluate_policy_notification_with_missing_context_should_use_body_fallbacks() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID studentUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        insertUser(ownerUserId, "owner-absence-fallback@example.com", "Owner Absence Fallback");
        insertUser(studentUserId, "student-absence-fallback@example.com", "Student Absence Fallback");
        insertGroup(groupId, ownerUserId, "Fallback thông báo", "ABW006", "ACTIVE");
        insertMember(groupId, ownerUserId, "OWNER", "APPROVED");
        insertMember(groupId, studentUserId, "MEMBER", "APPROVED");

        Instant baseStartAt = Instant.now().plusSeconds(3600);
        seedClosedHistory(groupId, ownerUserId, studentUserId, baseStartAt, "PRESENT", "PRESENT", "PRESENT", "PRESENT", "ABSENT");

        attendancePolicyNotificationOrchestrator.reevaluateOne(groupId, studentUserId, null);

        Map<String, Object> row = findNotificationRowWithoutSession(studentUserId, "ATTENDANCE_POLICY_WARNING");
        String body = row.get("body").toString();
        assertContains(body, "buổi học này");
        assertContains(body, "môn INT1348");
        assertContains(body, "lớp D22CQCNPM02-N - Fallback thông báo");
        assertContains(body, "Giảng viên: Owner Absence Fallback");
        assertContains(body, "1/5");
        assertContains(body, "20%");
        assertContains(body, "Ngưỡng cảnh báo của lớp là vắng từ 20% hoặc tỷ lệ điểm danh dưới 80%");
        assertDoesNotContain(body, "null");
        assertDoesNotContain(body, "undefined");

        JsonNode payload = notificationPayload(row);
        assertEquals(groupId.toString(), payload.get("groupId").asText());
        assertEquals("Fallback thông báo", payload.get("groupName").asText());
        assertEquals("Fallback thông báo", payload.get("className").asText());
        assertTrue(payload.get("courseName").isNull());
        assertTrue(payload.get("subjectName").isNull());
        assertTrue(payload.get("courseId").isNull());
        assertEquals("INT1348", payload.get("courseCode").asText());
        assertEquals("INT1348", payload.get("subjectCode").asText());
        assertEquals("ABW006", payload.get("groupCode").asText());
        assertEquals("D22CQCNPM02-N", payload.get("classCode").asText());
        assertTrue(payload.get("sessionId").isNull());
        assertTrue(payload.get("triggerSessionId").isNull());
        assertTrue(payload.get("sessionTitle").isNull());
        assertTrue(payload.get("sessionName").isNull());
        assertEquals(ownerUserId.toString(), payload.get("lecturerId").asText());
        assertEquals("Owner Absence Fallback", payload.get("lecturerName").asText());
        assertEquals("owner-absence-fallback@example.com", payload.get("lecturerEmail").asText());
        assertTrue(payload.get("attendanceStatus").isNull());
        assertEquals("WARNING", payload.get("policyStatus").asText());
        assertEquals(1, payload.get("absenceSessions").size());
    }

    @Test
    void close_session_present_late_excused_students_should_not_receive_absence_warning() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID presentUserId = UUID.randomUUID();
        UUID lateUserId = UUID.randomUUID();
        UUID excusedUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        insertUser(ownerUserId, "owner-no-absence@example.com", "Owner No Absence");
        insertUser(presentUserId, "present-no-absence@example.com", "Present No Absence");
        insertUser(lateUserId, "late-no-absence@example.com", "Late No Absence");
        insertUser(excusedUserId, "excused-no-absence@example.com", "Excused No Absence");
        insertGroup(groupId, ownerUserId, "Không thông báo vắng", "ABW004", "ACTIVE");
        insertMember(groupId, ownerUserId, "OWNER", "APPROVED");
        insertMember(groupId, presentUserId, "MEMBER", "APPROVED");
        insertMember(groupId, lateUserId, "MEMBER", "APPROVED");
        insertMember(groupId, excusedUserId, "MEMBER", "APPROVED");

        Instant startAt = Instant.now().plusSeconds(3600);
        insertSession(sessionId, groupId, ownerUserId, "Phiên không vắng", "OPEN", startAt, null);
        insertAttendance(sessionId, presentUserId, "PRESENT");
        insertAttendance(sessionId, lateUserId, "LATE");
        insertAttendance(sessionId, excusedUserId, "EXCUSED");

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/close", sessionId)
                        .with(auth(ownerUserId)))
                .andExpect(status().isOk());

        assertEquals(0L, countNotification(presentUserId, "ABSENCE_WARNING", sessionId));
        assertEquals(0L, countNotification(lateUserId, "ABSENCE_WARNING", sessionId));
        assertEquals(0L, countNotification(excusedUserId, "ABSENCE_WARNING", sessionId));
    }

    @Test
    void close_session_absence_warning_should_be_deduplicated_when_finalize_repeats() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID studentUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        insertUser(ownerUserId, "owner-absence-dedup@example.com", "Owner Absence Dedup");
        insertUser(studentUserId, "student-absence-dedup@example.com", "Student Absence Dedup");
        insertGroup(groupId, ownerUserId, "Chống trùng thông báo", "ABW005", "ACTIVE");
        insertMember(groupId, ownerUserId, "OWNER", "APPROVED");
        insertMember(groupId, studentUserId, "MEMBER", "APPROVED");

        Instant baseStartAt = Instant.now().plusSeconds(3600);
        seedClosedHistory(groupId, ownerUserId, studentUserId, baseStartAt, "PRESENT", "PRESENT", "PRESENT", "PRESENT", "PRESENT");
        insertSession(sessionId, groupId, ownerUserId, "Phiên vắng chống trùng", "OPEN", baseStartAt.plusSeconds(21600), null);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/close", sessionId)
                        .with(auth(ownerUserId)))
                .andExpect(status().isOk());

        attendancePolicyNotificationOrchestrator.reevaluateClosedSession(groupId, sessionId);
        attendancePolicyNotificationOrchestrator.reevaluateClosedSession(groupId, sessionId);

        assertEquals(1L, countNotification(studentUserId, "ABSENCE_WARNING", sessionId));
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

    private void insertAttendance(UUID sessionId, UUID userId, String attendanceStatus) {
        jdbcTemplate.update(
                """
                INSERT INTO session_attendance (
                    session_id,
                    user_id,
                    attendance_status,
                    check_in_at,
                    check_in_method,
                    qr_token_id,
                    device_id,
                    ip_address,
                    user_agent,
                    geo_lat,
                    geo_lng,
                    distance_meter,
                    suspicious_flag,
                    suspicious_reason,
                    excused_by_request_id,
                    created_at,
                    updated_at
                ) VALUES (
                    UUID_TO_BIN(?, 1),
                    UUID_TO_BIN(?, 1),
                    ?,
                    CASE WHEN ? IN ('PRESENT', 'LATE') THEN UTC_TIMESTAMP(6) ELSE NULL END,
                    'MANUAL',
                    NULL,
                    NULL,
                    NULL,
                    NULL,
                    NULL,
                    NULL,
                    NULL,
                    0,
                    NULL,
                    NULL,
                    UTC_TIMESTAMP(6),
                    UTC_TIMESTAMP(6)
                )
                """,
                sessionId.toString(),
                userId.toString(),
                attendanceStatus,
                attendanceStatus
        );
    }

    private void seedClosedHistory(
            UUID groupId,
            UUID ownerUserId,
            UUID studentUserId,
            Instant firstStartAt,
            String... attendanceStatuses
    ) {
        for (int i = 0; i < attendanceStatuses.length; i++) {
            UUID historySessionId = UUID.randomUUID();
            Instant startAt = firstStartAt.plusSeconds(i * 3600L);
            insertSession(
                    historySessionId,
                    groupId,
                    ownerUserId,
                    "Lịch sử điểm danh " + (i + 1),
                    "CLOSED",
                    startAt,
                    startAt.plusSeconds(5400)
            );
            insertAttendance(historySessionId, studentUserId, attendanceStatuses[i]);
        }
    }

    private long countNotification(UUID recipientUserId, String type, UUID sessionId) {
        Number n = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM notifications
                WHERE recipient_user_id = UUID_TO_BIN(?, 1)
                  AND type = ?
                  AND session_id = UUID_TO_BIN(?, 1)
                """,
                Number.class,
                recipientUserId.toString(),
                type,
                sessionId.toString()
        );
        return n == null ? 0L : n.longValue();
    }

    private long countAnyPolicyNotification(UUID recipientUserId, UUID sessionId) {
        Number n = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM notifications
                WHERE recipient_user_id = UUID_TO_BIN(?, 1)
                  AND session_id = UUID_TO_BIN(?, 1)
                  AND type IN (
                    'ATTENDANCE_POLICY_WARNING',
                    'ATTENDANCE_POLICY_CRITICAL',
                    'ATTENDANCE_POLICY_EXAM_BANNED'
                  )
                """,
                Number.class,
                recipientUserId.toString(),
                sessionId.toString()
        );
        return n == null ? 0L : n.longValue();
    }

    private Map<String, Object> findNotificationRow(UUID recipientUserId, String type, UUID sessionId) {
        return jdbcTemplate.queryForMap(
                """
                SELECT
                    BIN_TO_UUID(id, 1) AS id,
                    title,
                    body,
                    severity,
                    source_type,
                    BIN_TO_UUID(source_ref_id, 1) AS source_ref_id,
                    dedup_key,
                    CAST(payload_json AS CHAR) AS payload_json
                FROM notifications
                WHERE recipient_user_id = UUID_TO_BIN(?, 1)
                  AND type = ?
                  AND session_id = UUID_TO_BIN(?, 1)
                ORDER BY created_at DESC
                LIMIT 1
                """,
                recipientUserId.toString(),
                type,
                sessionId.toString()
        );
    }

    private Map<String, Object> findNotificationRowWithoutSession(UUID recipientUserId, String type) {
        return jdbcTemplate.queryForMap(
                """
                SELECT
                    BIN_TO_UUID(id, 1) AS id,
                    title,
                    body,
                    severity,
                    source_type,
                    source_ref_id,
                    dedup_key,
                    CAST(payload_json AS CHAR) AS payload_json
                FROM notifications
                WHERE recipient_user_id = UUID_TO_BIN(?, 1)
                  AND type = ?
                  AND session_id IS NULL
                ORDER BY created_at DESC
                LIMIT 1
                """,
                recipientUserId.toString(),
                type
        );
    }

    private JsonNode notificationPayload(Map<String, Object> notificationRow) throws Exception {
        return objectMapper.readTree(notificationRow.get("payload_json").toString());
    }

    private String bodyStartAt(Instant instant) {
        return NOTIFICATION_BODY_DATE.format(instant.atZone(NOTIFICATION_BODY_ZONE))
                + " "
                + NOTIFICATION_BODY_TIME.format(instant.atZone(NOTIFICATION_BODY_ZONE));
    }

    private String decimalText(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return node.decimalValue().stripTrailingZeros().toPlainString();
    }

    private boolean absenceSessionsContainTitle(JsonNode absenceSessions, String title) {
        for (JsonNode absenceSession : absenceSessions) {
            if (title.equals(absenceSession.get("sessionTitle").asText(null))) {
                return true;
            }
        }
        return false;
    }

    private void assertContains(String value, String expected) {
        assertTrue(value.contains(expected), () -> "Expected text to contain [" + expected + "] but was [" + value + "]");
    }

    private void assertDoesNotContain(String value, String unexpected) {
        assertTrue(!value.contains(unexpected), () -> "Expected text not to contain [" + unexpected + "] but was [" + value + "]");
    }

    private void cleanupTables() {
        jdbcTemplate.update("DELETE FROM notification_deliveries");
        jdbcTemplate.update("DELETE FROM notifications");
        jdbcTemplate.update("DELETE FROM session_attendance");
        jdbcTemplate.update("DELETE FROM attendance_policies");
        jdbcTemplate.update("DELETE FROM attendance_sessions");
        jdbcTemplate.update("DELETE FROM group_members");
        jdbcTemplate.update("DELETE FROM class_groups");
        jdbcTemplate.update("DELETE FROM users");
    }
}
