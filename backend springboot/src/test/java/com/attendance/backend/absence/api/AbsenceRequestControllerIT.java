package com.attendance.backend.absence.api;

import com.attendance.backend.security.UserPrincipal;
import com.attendance.backend.support.AbstractMySqlIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AbsenceRequestControllerIT extends AbstractMySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JavaMailSender javaMailSender;

    private UUID ownerId;
    private UUID cohostId;
    private UUID member1Id;
    private UUID member2Id;
    private UUID g2Id;
    private UUID g2OpenSessionId;
    private UUID seededApprovedRequestId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        cohostId = UUID.randomUUID();
        member1Id = UUID.randomUUID();
        member2Id = UUID.randomUUID();

        g2Id = UUID.randomUUID();
        g2OpenSessionId = UUID.randomUUID();
        seededApprovedRequestId = UUID.randomUUID();

        seedUser(ownerId, "owner@it.local");
        seedUser(cohostId, "cohost@it.local");
        seedUser(member1Id, "member1@it.local");
        seedUser(member2Id, "member2@it.local");

        seedGroup(g2Id, ownerId, "ENG102-2026", "English 102");
        seedApprovedOwnerMembership(g2Id, ownerId);
        seedApprovedCohostMembership(g2Id, cohostId);
        seedApprovedMemberMembership(g2Id, member1Id);
        seedApprovedMemberMembership(g2Id, member2Id);

        seedOpenSession(g2OpenSessionId, g2Id, ownerId, "G2 - OPEN (Live)");

        seedSessionAttendance(g2OpenSessionId, member1Id, "ABSENT");
        seedSessionAttendance(g2OpenSessionId, member2Id, "ABSENT");

        UUID approvedSessionId = UUID.randomUUID();
        UUID approvedStudentId = UUID.randomUUID();

        seedUser(approvedStudentId, "approved-member@it.local");
        seedApprovedMemberMembership(g2Id, approvedStudentId);
        seedClosedSession(approvedSessionId, g2Id, ownerId, "G2 - APPROVED");
        seedSessionAttendance(approvedSessionId, approvedStudentId, "EXCUSED");

        seedApprovedAbsenceRequest(
                seededApprovedRequestId,
                g2Id,
                approvedStudentId,
                approvedSessionId,
                ownerId
        );

        jdbcTemplate.update("""
            update session_attendance
            set excused_by_request_id = UUID_TO_BIN(?, 1)
            where session_id = UUID_TO_BIN(?, 1)
              and user_id = UUID_TO_BIN(?, 1)
        """, seededApprovedRequestId.toString(), approvedSessionId.toString(), approvedStudentId.toString());
    }

    @Test
    @DisplayName("student submit request thành công")
    void student_submit_request_success() throws Exception {
        String body = """
            {
              "linkedSessionId": "%s",
              "reason": "Sick leave",
              "evidenceUrl": "s3://bucket/evidence-001.jpg"
            }
            """.formatted(g2OpenSessionId);

        mockMvc.perform(post("/api/v1/groups/{groupId}/absence-requests", g2Id)
                        .with(auth(member1Id))
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(handler().handlerType(AbsenceRequestController.class))
                .andExpect(handler().methodName("create"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.groupId").value(g2Id.toString()))
                .andExpect(jsonPath("$.requesterUserId").value(member1Id.toString()))
                .andExpect(jsonPath("$.linkedSessionId").value(g2OpenSessionId.toString()))
                .andExpect(jsonPath("$.requestStatus").value("PENDING"));

        long count = count("""
            select count(*)
            from absence_requests
            where group_id = UUID_TO_BIN(?, 1)
              and requester_user_id = UUID_TO_BIN(?, 1)
              and linked_session_id = UUID_TO_BIN(?, 1)
              and request_status = 'PENDING'
        """, g2Id, member1Id, g2OpenSessionId);

        assertThat(count).isEqualTo(1L);

        long eventCount = count("""
            select count(*)
            from attendance_events
            where session_id = UUID_TO_BIN(?, 1)
              and user_id = UUID_TO_BIN(?, 1)
              and event_type = 'ABSENCE_REQUEST_CREATED'
        """, g2OpenSessionId, member1Id);

        assertThat(eventCount).isEqualTo(1L);
    }

    @Test
    @DisplayName("owner approve thành công -> attendance thành EXCUSED")
    void owner_approve_success_marks_excused() throws Exception {
        UUID requestId = createPendingRequest(member1Id, g2Id, g2OpenSessionId);

        String reviewBody = """
            {
              "action": "APPROVE",
              "reviewerNote": "Accepted"
            }
            """;

        mockMvc.perform(patch("/api/v1/absence-requests/{requestId}/review", requestId)
                        .with(auth(ownerId))
                        .contentType(APPLICATION_JSON)
                        .content(reviewBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(requestId.toString()))
                .andExpect(jsonPath("$.requestStatus").value("APPROVED"))
                .andExpect(jsonPath("$.reviewerUserId").value(ownerId.toString()));

        String attendanceStatus = jdbcTemplate.queryForObject("""
            select attendance_status
            from session_attendance
            where session_id = UUID_TO_BIN(?, 1)
              and user_id = UUID_TO_BIN(?, 1)
        """, String.class, g2OpenSessionId.toString(), member1Id.toString());

        String excusedByRequestId = jdbcTemplate.queryForObject("""
            select BIN_TO_UUID(excused_by_request_id, 1)
            from session_attendance
            where session_id = UUID_TO_BIN(?, 1)
              and user_id = UUID_TO_BIN(?, 1)
        """, String.class, g2OpenSessionId.toString(), member1Id.toString());

        assertThat(attendanceStatus).isEqualTo("EXCUSED");
        assertThat(excusedByRequestId).isEqualTo(requestId.toString());

        long markExcusedCount = count("""
            select count(*)
            from attendance_events
            where session_id = UUID_TO_BIN(?, 1)
              and user_id = UUID_TO_BIN(?, 1)
              and event_type = 'MARK_EXCUSED'
        """, g2OpenSessionId, member1Id);

        long approveEventCount = count("""
            select count(*)
            from attendance_events
            where session_id = UUID_TO_BIN(?, 1)
              and user_id = UUID_TO_BIN(?, 1)
              and event_type = 'ABSENCE_REQUEST_APPROVED'
        """, g2OpenSessionId, member1Id);

        assertThat(markExcusedCount).isEqualTo(1L);
        assertThat(approveEventCount).isEqualTo(1L);
    }

    @Test
    @DisplayName("owner reject thành công -> không làm bẩn current attendance")
    void owner_reject_success() throws Exception {
        UUID requestId = createPendingRequest(member1Id, g2Id, g2OpenSessionId);

        String reviewBody = """
            {
              "action": "REJECT",
              "reviewerNote": "Not enough evidence"
            }
            """;

        mockMvc.perform(patch("/api/v1/absence-requests/{requestId}/review", requestId)
                        .with(auth(ownerId))
                        .contentType(APPLICATION_JSON)
                        .content(reviewBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestStatus").value("REJECTED"));

        String attendanceStatus = jdbcTemplate.queryForObject("""
            select attendance_status
            from session_attendance
            where session_id = UUID_TO_BIN(?, 1)
              and user_id = UUID_TO_BIN(?, 1)
        """, String.class, g2OpenSessionId.toString(), member1Id.toString());

        assertThat(attendanceStatus).isEqualTo("ABSENT");

        long markExcusedCount = count("""
            select count(*)
            from attendance_events
            where session_id = UUID_TO_BIN(?, 1)
              and user_id = UUID_TO_BIN(?, 1)
              and event_type = 'MARK_EXCUSED'
        """, g2OpenSessionId, member1Id);

        assertThat(markExcusedCount).isZero();
    }

    @Test
    @DisplayName("student cancel pending request thành công")
    void student_cancel_pending_request_success() throws Exception {
        UUID requestId = createPendingRequest(member1Id, g2Id, g2OpenSessionId);

        mockMvc.perform(post("/api/v1/absence-requests/{requestId}/cancel", requestId)
                        .with(auth(member1Id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestStatus").value("CANCELLED"));

        long cancelEventCount = count("""
            select count(*)
            from attendance_events
            where session_id = UUID_TO_BIN(?, 1)
              and user_id = UUID_TO_BIN(?, 1)
              and event_type = 'ABSENCE_REQUEST_CANCELLED'
        """, g2OpenSessionId, member1Id);

        assertThat(cancelEventCount).isEqualTo(1L);
    }

    @Test
    @DisplayName("revert excused thành công")
    void revert_excused_success() throws Exception {
        mockMvc.perform(post("/api/v1/absence-requests/{requestId}/revert", seededApprovedRequestId)
                        .with(auth(ownerId))
                        .contentType(APPLICATION_JSON)
                        .content("""
                            { "revertNote": "Reverted by lecturer" }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestStatus").value("REVERTED"));

        JsonNode row = attendanceRowByRequest(seededApprovedRequestId);
        assertThat(row.get("attendance_status").asText()).isEqualTo("ABSENT");
        assertThat(row.get("excused_by_request_id").isNull()).isTrue();

        long revertEventCount = count("""
            select count(*)
            from attendance_events ae
            join absence_requests ar on ar.linked_session_id = ae.session_id
            where ar.id = UUID_TO_BIN(?, 1)
              and ae.user_id = ar.requester_user_id
              and ae.event_type = 'REVERT_FROM_EXCUSED'
        """, seededApprovedRequestId);

        assertThat(revertEventCount).isEqualTo(1L);
    }

    @Test
    @DisplayName("no JWT -> 401")
    void no_jwt_401() throws Exception {
        mockMvc.perform(post("/api/v1/groups/{groupId}/absence-requests", g2Id)
                        .contentType(APPLICATION_JSON)
                        .content("""
                            { "linkedSessionId": "%s", "reason": "test" }
                        """.formatted(g2OpenSessionId)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("member thường review -> 403")
    void normal_member_review_forbidden() throws Exception {
        UUID requestId = createPendingRequest(member1Id, g2Id, g2OpenSessionId);

        mockMvc.perform(patch("/api/v1/absence-requests/{requestId}/review", requestId)
                        .with(auth(member2Id))
                        .contentType(APPLICATION_JSON)
                        .content("""
                            { "action": "APPROVE" }
                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("student khác xem request không phải của mình -> 404")
    void other_student_cannot_view_request() throws Exception {
        UUID requestId = createPendingRequest(member1Id, g2Id, g2OpenSessionId);

        mockMvc.perform(get("/api/v1/absence-requests/{requestId}", requestId)
                        .with(auth(member2Id)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("duplicate pending request -> 409")
    void duplicate_pending_request_conflict() throws Exception {
        createPendingRequest(member1Id, g2Id, g2OpenSessionId);

        mockMvc.perform(post("/api/v1/groups/{groupId}/absence-requests", g2Id)
                        .with(auth(member1Id))
                        .contentType(APPLICATION_JSON)
                        .content("""
                            {
                              "linkedSessionId": "%s",
                              "reason": "Second pending request"
                            }
                        """.formatted(g2OpenSessionId)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("session không tồn tại -> 404")
    void session_not_found() throws Exception {
        mockMvc.perform(post("/api/v1/groups/{groupId}/absence-requests", g2Id)
                        .with(auth(member1Id))
                        .contentType(APPLICATION_JSON)
                        .content("""
                            {
                              "linkedSessionId": "%s",
                              "reason": "Unknown session"
                            }
                        """.formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("attendance row không tồn tại -> 404")
    void attendance_row_not_found() throws Exception {
        jdbcTemplate.update("""
            delete from session_attendance
            where session_id = UUID_TO_BIN(?, 1)
              and user_id = UUID_TO_BIN(?, 1)
        """, g2OpenSessionId.toString(), member1Id.toString());

        mockMvc.perform(post("/api/v1/groups/{groupId}/absence-requests", g2Id)
                        .with(auth(member1Id))
                        .contentType(APPLICATION_JSON)
                        .content("""
                            {
                              "linkedSessionId": "%s",
                              "reason": "Need leave"
                            }
                        """.formatted(g2OpenSessionId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("approve request đã reject -> 409")
    void approve_rejected_request_conflict() throws Exception {
        UUID requestId = createPendingRequest(member1Id, g2Id, g2OpenSessionId);

        mockMvc.perform(patch("/api/v1/absence-requests/{requestId}/review", requestId)
                        .with(auth(ownerId))
                        .contentType(APPLICATION_JSON)
                        .content("{\"action\":\"REJECT\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/absence-requests/{requestId}/review", requestId)
                        .with(auth(ownerId))
                        .contentType(APPLICATION_JSON)
                        .content("{\"action\":\"APPROVE\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("cancel request đã approved -> 409")
    void cancel_approved_request_conflict() throws Exception {
        UUID requestId = createPendingRequest(member1Id, g2Id, g2OpenSessionId);

        mockMvc.perform(patch("/api/v1/absence-requests/{requestId}/review", requestId)
                        .with(auth(ownerId))
                        .contentType(APPLICATION_JSON)
                        .content("{\"action\":\"APPROVE\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/absence-requests/{requestId}/cancel", requestId)
                        .with(auth(member1Id)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("same approve no-op không spam event")
    void same_approve_noop_does_not_spam_event() throws Exception {
        UUID requestId = createPendingRequest(member1Id, g2Id, g2OpenSessionId);

        mockMvc.perform(patch("/api/v1/absence-requests/{requestId}/review", requestId)
                        .with(auth(ownerId))
                        .contentType(APPLICATION_JSON)
                        .content("{\"action\":\"APPROVE\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/absence-requests/{requestId}/review", requestId)
                        .with(auth(ownerId))
                        .contentType(APPLICATION_JSON)
                        .content("{\"action\":\"APPROVE\"}"))
                .andExpect(status().isOk());

        long markExcusedCount = count("""
            select count(*)
            from attendance_events
            where session_id = UUID_TO_BIN(?, 1)
              and user_id = UUID_TO_BIN(?, 1)
              and event_type = 'MARK_EXCUSED'
        """, g2OpenSessionId, member1Id);

        assertThat(markExcusedCount).isEqualTo(1L);
    }

    private UUID createPendingRequest(UUID requesterUserId, UUID groupId, UUID sessionId) throws Exception {
        String response = mockMvc.perform(post("/api/v1/groups/{groupId}/absence-requests", groupId)
                        .with(auth(requesterUserId))
                        .contentType(APPLICATION_JSON)
                        .content("""
                            {
                              "linkedSessionId": "%s",
                              "reason": "Medical leave",
                              "evidenceUrl": "s3://bucket/medical.jpg"
                            }
                        """.formatted(sessionId)))
                .andExpect(handler().handlerType(AbsenceRequestController.class))
                .andExpect(handler().methodName("create"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return UUID.fromString(objectMapper.readTree(response).get("id").asText());
    }

    private JsonNode attendanceRowByRequest(UUID requestId) throws Exception {
        String json = jdbcTemplate.queryForObject("""
            select json_object(
                'attendance_status', sa.attendance_status,
                'excused_by_request_id', case when sa.excused_by_request_id is null then null else BIN_TO_UUID(sa.excused_by_request_id, 1) end
            )
            from session_attendance sa
            join absence_requests ar
              on ar.linked_session_id = sa.session_id
             and ar.requester_user_id = sa.user_id
            where ar.id = UUID_TO_BIN(?, 1)
        """, String.class, requestId.toString());

        return objectMapper.readTree(json);
    }

    private RequestPostProcessor auth(UUID userId) {
        UserPrincipal principal = Mockito.mock(UserPrincipal.class);
        Mockito.when(principal.getUserId()).thenReturn(userId);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of()
        );

        return SecurityMockMvcRequestPostProcessors.authentication(authentication);
    }

    private long count(String sql, UUID arg1) {
        Number n = jdbcTemplate.queryForObject(sql, Number.class, arg1.toString());
        return n == null ? 0L : n.longValue();
    }

    private long count(String sql, UUID arg1, UUID arg2) {
        Number n = jdbcTemplate.queryForObject(sql, Number.class, arg1.toString(), arg2.toString());
        return n == null ? 0L : n.longValue();
    }

    private long count(String sql, UUID arg1, UUID arg2, UUID arg3) {
        Number n = jdbcTemplate.queryForObject(
                sql,
                Number.class,
                arg1.toString(),
                arg2.toString(),
                arg3.toString()
        );
        return n == null ? 0L : n.longValue();
    }

    private void seedUser(UUID id, String email) {
        jdbcTemplate.update("""
            insert into users (
                id,
                email,
                password_hash,
                full_name,
                status,
                created_at,
                updated_at
            ) values (
                UUID_TO_BIN(?, 1),
                ?,
                '$2a$10$test.hash.test.hash.test.hash.test.hash.test.hash.test',
                ?,
                'ACTIVE',
                UTC_TIMESTAMP(6),
                UTC_TIMESTAMP(6)
            )
        """, id.toString(), email, email);
    }

    private void seedGroup(UUID groupId, UUID ownerUserId, String code, String name) {
        String joinCode = "JOIN-" + code;
        jdbcTemplate.update("""
            insert into class_groups (
                id,
                owner_user_id,
                code,
                name,
                join_code,
                created_at,
                updated_at
            ) values (
                UUID_TO_BIN(?, 1),
                UUID_TO_BIN(?, 1),
                ?,
                ?,
                ?,
                UTC_TIMESTAMP(6),
                UTC_TIMESTAMP(6)
            )
        """, groupId.toString(), ownerUserId.toString(), code, name, joinCode);
    }

    private void seedApprovedOwnerMembership(UUID groupId, UUID userId) {
        seedMembership(groupId, userId, "OWNER");
    }

    private void seedApprovedCohostMembership(UUID groupId, UUID userId) {
        seedMembership(groupId, userId, "CO_HOST");
    }

    private void seedApprovedMemberMembership(UUID groupId, UUID userId) {
        seedMembership(groupId, userId, "MEMBER");
    }

    private void seedMembership(UUID groupId, UUID userId, String role) {
        jdbcTemplate.update("""
            insert into group_members (
                group_id,
                user_id,
                role,
                member_status,
                joined_at,
                created_at,
                updated_at
            ) values (
                UUID_TO_BIN(?, 1),
                UUID_TO_BIN(?, 1),
                ?,
                'APPROVED',
                UTC_TIMESTAMP(6),
                UTC_TIMESTAMP(6),
                UTC_TIMESTAMP(6)
            )
        """, groupId.toString(), userId.toString(), role);
    }

    private void seedOpenSession(UUID sessionId, UUID groupId, UUID createdByUserId, String title) {
        jdbcTemplate.update("""
            insert into attendance_sessions (
                id,
                group_id,
                title,
                session_date,
                start_at,
                end_at,
                checkin_open_at,
                checkin_close_at,
                status,
                allow_manual_override,
                late_after_minutes,
                time_window_minutes,
                qr_rotate_seconds,
                session_secret,
                note,
                created_by_user_id,
                created_at,
                updated_at,
                deleted_at
            ) values (
                UUID_TO_BIN(?, 1),
                UUID_TO_BIN(?, 1),
                ?,
                current_date(),
                UTC_TIMESTAMP(6),
                DATE_ADD(UTC_TIMESTAMP(6), interval 2 hour),
                DATE_SUB(UTC_TIMESTAMP(6), interval 15 minute),
                DATE_ADD(UTC_TIMESTAMP(6), interval 60 minute),
                'OPEN',
                true,
                15,
                60,
                30,
                'test-secret',
                'seeded by AbsenceRequestControllerIT',
                UUID_TO_BIN(?, 1),
                UTC_TIMESTAMP(6),
                UTC_TIMESTAMP(6),
                null
            )
        """, sessionId.toString(), groupId.toString(), title, createdByUserId.toString());
    }

    private void seedClosedSession(UUID sessionId, UUID groupId, UUID createdByUserId, String title) {
        jdbcTemplate.update("""
            insert into attendance_sessions (
                id,
                group_id,
                title,
                session_date,
                start_at,
                end_at,
                checkin_open_at,
                checkin_close_at,
                status,
                allow_manual_override,
                late_after_minutes,
                time_window_minutes,
                qr_rotate_seconds,
                session_secret,
                note,
                created_by_user_id,
                created_at,
                updated_at,
                deleted_at
            ) values (
                UUID_TO_BIN(?, 1),
                UUID_TO_BIN(?, 1),
                ?,
                current_date(),
                DATE_SUB(UTC_TIMESTAMP(6), interval 3 hour),
                DATE_SUB(UTC_TIMESTAMP(6), interval 1 hour),
                DATE_SUB(UTC_TIMESTAMP(6), interval 4 hour),
                DATE_SUB(UTC_TIMESTAMP(6), interval 2 hour),
                'CLOSED',
                true,
                15,
                60,
                30,
                'test-secret-closed',
                'seeded closed session by AbsenceRequestControllerIT',
                UUID_TO_BIN(?, 1),
                UTC_TIMESTAMP(6),
                UTC_TIMESTAMP(6),
                null
            )
        """, sessionId.toString(), groupId.toString(), title, createdByUserId.toString());
    }

    private void seedSessionAttendance(UUID sessionId, UUID userId, String status) {
        jdbcTemplate.update("""
            insert into session_attendance (
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
            ) values (
                UUID_TO_BIN(?, 1),
                UUID_TO_BIN(?, 1),
                ?,
                null,
                'MANUAL',
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                UTC_TIMESTAMP(6),
                UTC_TIMESTAMP(6)
            )
        """, sessionId.toString(), userId.toString(), status);
    }

    private void seedApprovedAbsenceRequest(
            UUID requestId,
            UUID groupId,
            UUID requesterUserId,
            UUID linkedSessionId,
            UUID reviewerUserId
    ) {
        jdbcTemplate.update("""
            insert into absence_requests (
                id,
                group_id,
                requester_user_id,
                linked_session_id,
                reason,
                evidence_url,
                request_status,
                reviewer_user_id,
                reviewer_note,
                reviewed_at,
                created_at,
                updated_at
            ) values (
                UUID_TO_BIN(?, 1),
                UUID_TO_BIN(?, 1),
                UUID_TO_BIN(?, 1),
                UUID_TO_BIN(?, 1),
                'Approved seed request',
                's3://bucket/approved-seed.jpg',
                'APPROVED',
                UUID_TO_BIN(?, 1),
                'Approved in seed',
                UTC_TIMESTAMP(6),
                UTC_TIMESTAMP(6),
                UTC_TIMESTAMP(6)
            )
        """, requestId.toString(), groupId.toString(), requesterUserId.toString(), linkedSessionId.toString(), reviewerUserId.toString());
    }
}
