package com.attendance.backend.attendance.api;

import com.attendance.backend.security.UserPrincipal;
import com.attendance.backend.security.jwt.JwtAuthenticationFilter;
import com.attendance.backend.support.AbstractMySqlIntegrationTest;
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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import static org.mockito.ArgumentMatchers.any;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AttendanceAdminManualMarkControllerIT extends AbstractMySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JavaMailSender javaMailSender;

    private UUID ownerId;
    private UUID cohostId;
    private UUID member1Id;
    private UUID member2Id;
    private UUID excusedMemberId;
    private UUID g2Id;
    private UUID g2OpenSessionId;

    @BeforeEach
    void setUp() throws Exception {
        Mockito.doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());

        ownerId = UUID.randomUUID();
        cohostId = UUID.randomUUID();
        member1Id = UUID.randomUUID();
        member2Id = UUID.randomUUID();
        excusedMemberId = UUID.randomUUID();

        g2Id = UUID.randomUUID();
        g2OpenSessionId = UUID.randomUUID();

        seedUser(ownerId, "owner@it.local");
        seedUser(cohostId, "cohost@it.local");
        seedUser(member1Id, "member1@it.local");
        seedUser(member2Id, "member2@it.local");
        seedUser(excusedMemberId, "excused-member@it.local");

        seedGroup(g2Id, ownerId, "ENG102-2026", "English 102");
        seedApprovedOwnerMembership(g2Id, ownerId);
        seedApprovedCohostMembership(g2Id, cohostId);
        seedApprovedMemberMembership(g2Id, member1Id);
        seedApprovedMemberMembership(g2Id, member2Id);
        seedApprovedMemberMembership(g2Id, excusedMemberId);

        seedOpenSession(g2OpenSessionId, g2Id, ownerId, "G2 - OPEN (Live)");

        seedSessionAttendance(g2OpenSessionId, member1Id, "ABSENT");
        seedSessionAttendance(g2OpenSessionId, member2Id, "ABSENT");
        seedSessionAttendance(g2OpenSessionId, excusedMemberId, "EXCUSED");
    }

    @Test
    @DisplayName("owner manual mark PRESENT thành công")
    void owner_manual_mark_present_success() throws Exception {
        mockMvc.perform(post("/api/v1/sessions/{sessionId}/attendance/{userId}", g2OpenSessionId, member1Id)
                        .with(auth(ownerId))
                        .contentType(APPLICATION_JSON)
                        .content("""
                            {
                              "status": "PRESENT",
                              "note": "Camera issue, lecturer confirmed attendance"
                            }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(g2OpenSessionId.toString()))
                .andExpect(jsonPath("$.userId").value(member1Id.toString()))
                .andExpect(jsonPath("$.attendanceStatus").value("PRESENT"))
                .andExpect(jsonPath("$.checkInMethod").value("MANUAL"))
                .andExpect(jsonPath("$.qrTokenId").doesNotExist());

        String status = jdbcTemplate.queryForObject("""
            select attendance_status
            from session_attendance
            where session_id = UUID_TO_BIN(?, 1)
              and user_id = UUID_TO_BIN(?, 1)
        """, String.class, g2OpenSessionId.toString(), member1Id.toString());

        String checkInMethod = jdbcTemplate.queryForObject("""
            select check_in_method
            from session_attendance
            where session_id = UUID_TO_BIN(?, 1)
              and user_id = UUID_TO_BIN(?, 1)
        """, String.class, g2OpenSessionId.toString(), member1Id.toString());

        assertThat(status).isEqualTo("PRESENT");
        assertThat(checkInMethod).isEqualTo("MANUAL");

        long eventCount = count("""
            select count(*)
            from attendance_events
            where session_id = UUID_TO_BIN(?, 1)
              and user_id = UUID_TO_BIN(?, 1)
              and event_type = 'MARK_MANUAL_PRESENT'
        """, g2OpenSessionId, member1Id);

        assertThat(eventCount).isEqualTo(1L);
    }

    @Test
    @DisplayName("cohost manual mark LATE thành công")
    void cohost_manual_mark_late_success() throws Exception {
        mockMvc.perform(post("/api/v1/sessions/{sessionId}/attendance/{userId}", g2OpenSessionId, member1Id)
                        .with(auth(cohostId))
                        .contentType(APPLICATION_JSON)
                        .content("""
                            {
                              "status": "LATE",
                              "note": "Arrived late, manually recorded"
                            }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendanceStatus").value("LATE"))
                .andExpect(jsonPath("$.checkInMethod").value("MANUAL"));

        long eventCount = count("""
            select count(*)
            from attendance_events
            where session_id = UUID_TO_BIN(?, 1)
              and user_id = UUID_TO_BIN(?, 1)
              and event_type = 'MARK_MANUAL_LATE'
        """, g2OpenSessionId, member1Id);

        assertThat(eventCount).isEqualTo(1L);
    }

    @Test
    @DisplayName("manual mark ABSENT clears checkin fields")
    void manual_mark_absent_clears_checkin_fields() throws Exception {
        mockMvc.perform(post("/api/v1/sessions/{sessionId}/attendance/{userId}", g2OpenSessionId, member1Id)
                        .with(auth(ownerId))
                        .contentType(APPLICATION_JSON)
                        .content("""
                            { "status": "PRESENT", "note": "seed manual present first" }
                        """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/attendance/{userId}", g2OpenSessionId, member1Id)
                        .with(auth(ownerId))
                        .contentType(APPLICATION_JSON)
                        .content("""
                            { "status": "ABSENT", "note": "set absent manually" }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendanceStatus").value("ABSENT"))
                .andExpect(jsonPath("$.checkInAt").doesNotExist())
                .andExpect(jsonPath("$.qrTokenId").doesNotExist());

        String status = jdbcTemplate.queryForObject("""
            select attendance_status
            from session_attendance
            where session_id = UUID_TO_BIN(?, 1)
              and user_id = UUID_TO_BIN(?, 1)
        """, String.class, g2OpenSessionId.toString(), member1Id.toString());

        String qrTokenId = jdbcTemplate.queryForObject("""
            select qr_token_id
            from session_attendance
            where session_id = UUID_TO_BIN(?, 1)
              and user_id = UUID_TO_BIN(?, 1)
        """, String.class, g2OpenSessionId.toString(), member1Id.toString());

        assertThat(status).isEqualTo("ABSENT");
        assertThat(qrTokenId).isNull();

        long eventCount = count("""
            select count(*)
            from attendance_events
            where session_id = UUID_TO_BIN(?, 1)
              and user_id = UUID_TO_BIN(?, 1)
              and event_type = 'MARK_MANUAL_ABSENT'
        """, g2OpenSessionId, member1Id);

        assertThat(eventCount).isEqualTo(1L);
    }

    @Test
    @DisplayName("member thường manual mark -> 403")
    void normal_member_manual_mark_forbidden() throws Exception {
        mockMvc.perform(post("/api/v1/sessions/{sessionId}/attendance/{userId}", g2OpenSessionId, member1Id)
                        .with(auth(member2Id))
                        .contentType(APPLICATION_JSON)
                        .content("""
                        {
                          "status": "PRESENT",
                          "note": "normal member is not allowed to manual mark"
                        }
                    """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("no JWT -> 401")
    void no_jwt_401() throws Exception {
        mockMvc.perform(post("/api/v1/sessions/{sessionId}/attendance/{userId}", g2OpenSessionId, member1Id)
                        .contentType(APPLICATION_JSON)
                        .content("""
                            { "status": "PRESENT" }
                        """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("manual override disabled -> 409")
    void manual_override_disabled_conflict() throws Exception {
        jdbcTemplate.update("""
        update attendance_sessions
        set allow_manual_override = 0
        where id = UUID_TO_BIN(?, 1)
    """, g2OpenSessionId.toString());

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/attendance/{userId}", g2OpenSessionId, member1Id)
                        .with(auth(ownerId))
                        .contentType(APPLICATION_JSON)
                        .content("""
                        {
                          "status": "PRESENT",
                          "note": "manual override disabled should conflict"
                        }
                    """))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("manual mark row đang EXCUSED -> 409")
    void manual_mark_excused_row_conflict() throws Exception {
        mockMvc.perform(post("/api/v1/sessions/{sessionId}/attendance/{userId}", g2OpenSessionId, excusedMemberId)
                        .with(auth(ownerId))
                        .contentType(APPLICATION_JSON)
                        .content("""
                            { "status": "PRESENT", "note": "should fail because row is excused" }
                        """))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("invalid status EXCUSED at request binding -> 400")
    void invalid_status_excused_bad_request() throws Exception {
        mockMvc.perform(post("/api/v1/sessions/{sessionId}/attendance/{userId}", g2OpenSessionId, member1Id)
                        .with(auth(ownerId))
                        .contentType(APPLICATION_JSON)
                        .content("""
                            { "status": "EXCUSED" }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("same manual mark no-op does not spam event")
    void same_manual_mark_noop_no_event_spam() throws Exception {
        mockMvc.perform(post("/api/v1/sessions/{sessionId}/attendance/{userId}", g2OpenSessionId, member1Id)
                        .with(auth(ownerId))
                        .contentType(APPLICATION_JSON)
                        .content("""
                            { "status": "PRESENT", "note": "first mark" }
                        """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/attendance/{userId}", g2OpenSessionId, member1Id)
                        .with(auth(ownerId))
                        .contentType(APPLICATION_JSON)
                        .content("""
                            { "status": "PRESENT", "note": "second identical mark" }
                        """))
                .andExpect(status().isOk());

        long eventCount = count("""
            select count(*)
            from attendance_events
            where session_id = UUID_TO_BIN(?, 1)
              and user_id = UUID_TO_BIN(?, 1)
              and event_type = 'MARK_MANUAL_PRESENT'
        """, g2OpenSessionId, member1Id);

        assertThat(eventCount).isEqualTo(1L);
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

    private long count(String sql, UUID arg1, UUID arg2) {
        Number n = jdbcTemplate.queryForObject(
                sql,
                Number.class,
                arg1.toString(),
                arg2.toString()
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
                'seeded by AttendanceAdminManualMarkControllerIT',
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
}