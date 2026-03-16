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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

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
    private UUID g2OpenSessionId;

    @BeforeEach
    void setUp() {
        ownerId = userId("owner@demo.local");
        cohostId = userId("cohost@demo.local");
        member1Id = userId("member1@demo.local");
        member2Id = userId("member2@demo.local");
        g2OpenSessionId = sessionIdByTitle("ENG102-2026", "G2 - OPEN (Live)");
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
                            { "status": "PRESENT" }
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
                            { "status": "PRESENT" }
                        """))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("manual mark row đang EXCUSED -> 409")
    void manual_mark_excused_row_conflict() throws Exception {
        UUID excusedSessionId = uuid("""
            select BIN_TO_UUID(session_id, 1)
            from session_attendance
            where attendance_status = 'EXCUSED'
            limit 1
        """);

        UUID excusedUserId = uuid("""
            select BIN_TO_UUID(user_id, 1)
            from session_attendance
            where attendance_status = 'EXCUSED'
            limit 1
        """);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/attendance/{userId}", excusedSessionId, excusedUserId)
                        .with(auth(ownerId))
                        .contentType(APPLICATION_JSON)
                        .content("""
                            { "status": "PRESENT" }
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

    private UUID userId(String email) {
        return uuid("""
            select BIN_TO_UUID(id, 1)
            from users
            where email_norm = lower(trim(?))
        """, email);
    }

    private UUID sessionIdByTitle(String groupCode, String title) {
        return uuid("""
            select BIN_TO_UUID(s.id, 1)
            from attendance_sessions s
            join class_groups g on g.id = s.group_id
            where g.code = ?
              and s.title = ?
            limit 1
        """, groupCode, title);
    }

    private UUID uuid(String sql, Object... args) {
        return UUID.fromString(jdbcTemplate.queryForObject(sql, String.class, args));
    }

    private long count(String sql, UUID arg1, UUID arg2) {
        Number n = jdbcTemplate.queryForObject(sql, Number.class, arg1.toString(), arg2.toString());
        return n == null ? 0L : n.longValue();
    }
}