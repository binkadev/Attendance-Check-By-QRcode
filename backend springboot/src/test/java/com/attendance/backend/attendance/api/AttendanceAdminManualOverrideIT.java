package com.attendance.backend.attendance.api;

import com.attendance.backend.attendance.service.AttendancePolicyNotificationOrchestrator;
import com.attendance.backend.security.UserPrincipal;
import com.attendance.backend.support.AbstractMySqlIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AttendanceAdminManualOverrideIT extends AbstractMySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AttendancePolicyNotificationOrchestrator attendancePolicyNotificationOrchestrator;

    @MockitoBean
    private JavaMailSender javaMailSender;

    record Scenario(
            UUID ownerId,
            String ownerEmail,
            UUID targetStudentId,
            String targetStudentEmail,
            UUID groupId,
            UUID sessionId
    ) {
    }

    @Test
    void manual_mark_closed_session_should_create_warning_policy_notification() throws Exception {
        Scenario s = seedScenario("CLOSED", true);

        replacePolicy(
                s.groupId(),
                s.ownerId(),
                new BigDecimal("1.00"),
                new BigDecimal("80.00"),
                new BigDecimal("75.00"),
                10,
                20
        );

        seedClosedSessionWithAttendance(s, "Policy closed #1", "PRESENT");
        seedClosedSessionWithAttendance(s, "Policy closed #2", "PRESENT");
        seedClosedSessionWithAttendance(s, "Policy closed #3", "PRESENT");
        seedClosedSessionWithAttendance(s, "Policy closed #4", "PRESENT");

        makeAttendancePresentQrLike(s.sessionId(), s.targetStudentId(), s.ownerId());

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/attendance/{userId}", s.sessionId(), s.targetStudentId())
                        .with(authAs(s.ownerId(), s.ownerEmail()))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "status": "ABSENT",
                                  "note": "Manual correction to absent for policy warning"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendanceStatus").value("ABSENT"));

        assertThat(countPolicyNotifications(
                s.targetStudentId(),
                "ATTENDANCE_POLICY_WARNING",
                s.sessionId()
        )).isEqualTo(1L);

        Map<String, Object> row = findPolicyNotificationRow(
                s.targetStudentId(),
                "ATTENDANCE_POLICY_WARNING",
                s.sessionId()
        );

        assertThat(row.get("severity")).isEqualTo("WARNING");
        assertThat(row.get("source_type")).isEqualTo("ATTENDANCE_POLICY");
        assertThat(row.get("source_ref_id")).isEqualTo(s.sessionId().toString());
        assertThat((String) row.get("dedup_key")).isNotBlank().hasSize(64);

        JsonNode payload = objectMapper.readTree((String) row.get("payload_json"));
        assertThat(payload.get("policyStatus").asText()).isEqualTo("WARNING");
        assertThat(payload.get("riskLevel").asText()).isEqualTo("WARNING");
        assertThat(payload.get("examEligibility").asText()).isEqualTo("AT_RISK");
        assertThat(payload.get("eligibleSessionCount").asLong()).isEqualTo(5L);
        assertThat(payload.get("presentCount").asLong()).isEqualTo(4L);
        assertThat(payload.get("absentCount").asLong()).isEqualTo(1L);
        assertThat(payload.get("attendanceRate").decimalValue()).isEqualByComparingTo("80.00");
        assertThat(payload.get("absenceRate").decimalValue()).isEqualByComparingTo("20.00");
    }

    @Test
    void manual_mark_closed_session_should_create_critical_policy_notification() throws Exception {
        Scenario s = seedScenario("CLOSED", true);

        replacePolicy(
                s.groupId(),
                s.ownerId(),
                new BigDecimal("1.00"),
                new BigDecimal("80.00"),
                new BigDecimal("75.00"),
                10,
                20
        );

        seedClosedSessionWithAttendance(s, "Policy critical #1", "PRESENT");
        seedClosedSessionWithAttendance(s, "Policy critical #2", "PRESENT");
        seedClosedSessionWithAttendance(s, "Policy critical #3", "PRESENT");

        makeAttendancePresentQrLike(s.sessionId(), s.targetStudentId(), s.ownerId());

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/attendance/{userId}", s.sessionId(), s.targetStudentId())
                        .with(authAs(s.ownerId(), s.ownerEmail()))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "status": "ABSENT",
                                  "note": "Manual correction to absent for critical policy"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendanceStatus").value("ABSENT"));

        assertThat(countPolicyNotifications(
                s.targetStudentId(),
                "ATTENDANCE_POLICY_CRITICAL",
                s.sessionId()
        )).isEqualTo(1L);

        Map<String, Object> row = findPolicyNotificationRow(
                s.targetStudentId(),
                "ATTENDANCE_POLICY_CRITICAL",
                s.sessionId()
        );

        assertThat(row.get("severity")).isEqualTo("CRITICAL");
        assertThat(row.get("source_type")).isEqualTo("ATTENDANCE_POLICY");
        assertThat(row.get("source_ref_id")).isEqualTo(s.sessionId().toString());

        JsonNode payload = objectMapper.readTree((String) row.get("payload_json"));
        assertThat(payload.get("policyStatus").asText()).isEqualTo("CRITICAL");
        assertThat(payload.get("riskLevel").asText()).isEqualTo("CRITICAL");
        assertThat(payload.get("examEligibility").asText()).isEqualTo("AT_RISK");
        assertThat(payload.get("eligibleSessionCount").asLong()).isEqualTo(4L);
        assertThat(payload.get("presentCount").asLong()).isEqualTo(3L);
        assertThat(payload.get("absentCount").asLong()).isEqualTo(1L);
        assertThat(payload.get("attendanceRate").decimalValue()).isEqualByComparingTo("75.00");
        assertThat(payload.get("absenceRate").decimalValue()).isEqualByComparingTo("25.00");
    }

    @Test
    void manual_mark_closed_session_should_create_exam_banned_policy_notification() throws Exception {
        Scenario s = seedScenario("CLOSED", true);

        replacePolicy(
                s.groupId(),
                s.ownerId(),
                new BigDecimal("1.00"),
                new BigDecimal("80.00"),
                new BigDecimal("75.00"),
                10,
                20
        );

        for (int i = 1; i <= 7; i++) {
            seedClosedSessionWithAttendance(s, "Policy exam present #" + i, "PRESENT");
        }
        seedClosedSessionWithAttendance(s, "Policy exam absent #1", "ABSENT");
        seedClosedSessionWithAttendance(s, "Policy exam absent #2", "ABSENT");

        makeAttendancePresentQrLike(s.sessionId(), s.targetStudentId(), s.ownerId());

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/attendance/{userId}", s.sessionId(), s.targetStudentId())
                        .with(authAs(s.ownerId(), s.ownerEmail()))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "status": "ABSENT",
                                  "note": "Manual correction to absent for exam ban policy"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendanceStatus").value("ABSENT"));

        assertThat(countPolicyNotifications(
                s.targetStudentId(),
                "ATTENDANCE_POLICY_EXAM_BANNED",
                s.sessionId()
        )).isEqualTo(1L);

        Map<String, Object> row = findPolicyNotificationRow(
                s.targetStudentId(),
                "ATTENDANCE_POLICY_EXAM_BANNED",
                s.sessionId()
        );

        assertThat(row.get("title")).isEqualTo("Không đủ điều kiện dự thi");
        assertThat(row.get("severity")).isEqualTo("CRITICAL");
        assertThat(row.get("source_type")).isEqualTo("ATTENDANCE_POLICY");
        assertThat(row.get("source_ref_id")).isEqualTo(s.sessionId().toString());

        JsonNode payload = objectMapper.readTree((String) row.get("payload_json"));
        assertThat(payload.get("sessionId").asText()).isEqualTo(s.sessionId().toString());
        assertThat(payload.get("sessionTitle").asText()).isEqualTo("CLOSED session");
        assertThat(payload.get("attendanceStatus").asText()).isEqualTo("ABSENT");
        assertThat(payload.get("policyStatus").asText()).isEqualTo("EXAM_BANNED");
        assertThat(payload.get("riskLevel").asText()).isEqualTo("EXAM_BANNED");
        assertThat(payload.get("examEligibility").asText()).isEqualTo("BANNED");
        assertThat(payload.get("eligibleSessionCount").asLong()).isEqualTo(10L);
        assertThat(payload.get("presentCount").asLong()).isEqualTo(7L);
        assertThat(payload.get("absentCount").asLong()).isEqualTo(3L);
        assertThat(payload.get("attendanceRate").decimalValue()).isEqualByComparingTo("70.00");
        assertThat(payload.get("absenceRate").decimalValue()).isEqualByComparingTo("30.00");
        assertThat(payload.get("warningThresholds").get("absenceRate").decimalValue()).isEqualByComparingTo("20.00");
        assertThat(payload.get("criticalThresholds").get("absenceRate").decimalValue()).isEqualByComparingTo("25.00");
        assertThat(payload.get("examBanThresholds").get("absenceRate").decimalValue()).isEqualByComparingTo("30.00");
    }

    @Test
    void manual_mark_open_session_should_not_create_policy_notification() throws Exception {
        Scenario s = seedScenario("OPEN", true);

        replacePolicy(
                s.groupId(),
                s.ownerId(),
                new BigDecimal("1.00"),
                new BigDecimal("80.00"),
                new BigDecimal("75.00"),
                10,
                20
        );

        seedClosedSessionWithAttendance(s, "Policy baseline #1", "PRESENT");
        seedClosedSessionWithAttendance(s, "Policy baseline #2", "PRESENT");
        seedClosedSessionWithAttendance(s, "Policy baseline #3", "PRESENT");

        makeAttendancePresentQrLike(s.sessionId(), s.targetStudentId(), s.ownerId());

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/attendance/{userId}", s.sessionId(), s.targetStudentId())
                        .with(authAs(s.ownerId(), s.ownerEmail()))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "status": "ABSENT",
                                  "note": "Open session should not trigger policy notification"
                                }
                                """))
                .andExpect(status().isOk());

        assertThat(countAnyPolicyNotifications(s.targetStudentId())).isZero();
    }

    @Test
    void reset_closed_session_should_create_warning_policy_notification() throws Exception {
        Scenario s = seedScenario("CLOSED", true);

        replacePolicy(
                s.groupId(),
                s.ownerId(),
                new BigDecimal("1.00"),
                new BigDecimal("80.00"),
                new BigDecimal("75.00"),
                10,
                20
        );

        seedClosedSessionWithAttendance(s, "Policy reset #1", "PRESENT");
        seedClosedSessionWithAttendance(s, "Policy reset #2", "PRESENT");
        seedClosedSessionWithAttendance(s, "Policy reset #3", "PRESENT");
        seedClosedSessionWithAttendance(s, "Policy reset #4", "PRESENT");

        makeAttendancePresentQrLike(s.sessionId(), s.targetStudentId(), s.ownerId());

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/attendance/{userId}/reset", s.sessionId(), s.targetStudentId())
                        .with(authAs(s.ownerId(), s.ownerEmail()))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendanceStatus").value("ABSENT"));

        assertThat(countPolicyNotifications(
                s.targetStudentId(),
                "ATTENDANCE_POLICY_WARNING",
                s.sessionId()
        )).isEqualTo(1L);
    }

    @Test
    void policy_notification_should_not_be_created_when_absence_below_warning() {
        Scenario s = seedScenario("CLOSED", true);

        replacePolicy(
                s.groupId(),
                s.ownerId(),
                new BigDecimal("1.00"),
                new BigDecimal("80.00"),
                new BigDecimal("75.00"),
                10,
                20
        );

        for (int i = 1; i <= 5; i++) {
            seedClosedSessionWithAttendance(s, "Policy normal present #" + i, "PRESENT");
        }

        attendancePolicyNotificationOrchestrator.reevaluateOne(s.groupId(), s.targetStudentId(), s.sessionId());

        assertThat(countAnyPolicyNotifications(s.targetStudentId())).isZero();
    }

    @Test
    void policy_notification_should_exclude_excused_sessions_from_absence_rate_denominator() throws Exception {
        Scenario s = seedScenario("CLOSED", true);

        replacePolicy(
                s.groupId(),
                s.ownerId(),
                new BigDecimal("1.00"),
                new BigDecimal("80.00"),
                new BigDecimal("75.00"),
                10,
                20
        );

        seedClosedSessionWithAttendance(s, "Policy excused present #1", "PRESENT");
        seedClosedSessionWithAttendance(s, "Policy excused present #2", "PRESENT");
        seedClosedSessionWithAttendance(s, "Policy excused present #3", "PRESENT");
        seedClosedSessionWithAttendance(s, "Policy excused present #4", "PRESENT");
        seedClosedSessionWithAttendance(s, "Policy excused excluded", "EXCUSED");

        attendancePolicyNotificationOrchestrator.reevaluateOne(s.groupId(), s.targetStudentId(), s.sessionId());

        assertThat(countPolicyNotifications(
                s.targetStudentId(),
                "ATTENDANCE_POLICY_WARNING",
                s.sessionId()
        )).isEqualTo(1L);

        Map<String, Object> row = findPolicyNotificationRow(
                s.targetStudentId(),
                "ATTENDANCE_POLICY_WARNING",
                s.sessionId()
        );

        JsonNode payload = objectMapper.readTree((String) row.get("payload_json"));
        assertThat(payload.get("closedSessionCount").asLong()).isEqualTo(6L);
        assertThat(payload.get("eligibleSessionCount").asLong()).isEqualTo(5L);
        assertThat(payload.get("presentCount").asLong()).isEqualTo(4L);
        assertThat(payload.get("absentCount").asLong()).isEqualTo(1L);
        assertThat(payload.get("excusedCount").asLong()).isEqualTo(1L);
        assertThat(payload.get("absenceRate").decimalValue()).isEqualByComparingTo("20.00");
        assertThat(payload.get("policyStatus").asText()).isEqualTo("WARNING");
    }

    @Test
    @DisplayName("same source session reevaluate chỉ tạo 1 notification row")
    void policy_notification_dedup_same_source_session_should_create_single_row() {
        Scenario s = seedScenario("CLOSED", true);

        replacePolicy(
                s.groupId(),
                s.ownerId(),
                new BigDecimal("1.00"),
                new BigDecimal("80.00"),
                new BigDecimal("75.00"),
                10,
                20
        );

        seedClosedSessionWithAttendance(s, "Policy dedup #1", "PRESENT");
        seedClosedSessionWithAttendance(s, "Policy dedup #2", "PRESENT");
        seedClosedSessionWithAttendance(s, "Policy dedup #3", "PRESENT");
        seedClosedSessionWithAttendance(s, "Policy dedup #4", "PRESENT");

        attendancePolicyNotificationOrchestrator.reevaluateOne(s.groupId(), s.targetStudentId(), s.sessionId());
        attendancePolicyNotificationOrchestrator.reevaluateOne(s.groupId(), s.targetStudentId(), s.sessionId());

        assertThat(countPolicyNotifications(
                s.targetStudentId(),
                "ATTENDANCE_POLICY_WARNING",
                s.sessionId()
        )).isEqualTo(1L);
    }

    private Scenario seedScenario(String sessionStatus, boolean targetDefaultAbsent) {
        UUID ownerId = UUID.randomUUID();
        String ownerEmail = "owner-" + ownerId + "@it.local";

        UUID targetStudentId = UUID.randomUUID();
        String targetStudentEmail = "student-" + targetStudentId + "@it.local";

        UUID groupId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        seedUser(ownerId, ownerEmail);
        seedUser(targetStudentId, targetStudentEmail);

        // giữ code ngắn để không làm join_code bị tràn
        seedGroup(groupId, ownerId, "GRP001", "Policy Test Group");
        seedApprovedOwnerMembership(groupId, ownerId);
        seedApprovedMemberMembership(groupId, targetStudentId);

        insertSession(sessionId, groupId, ownerId, sessionStatus, true);
        insertAttendance(
                sessionId,
                targetStudentId,
                targetDefaultAbsent ? "ABSENT" : "PRESENT",
                "MANUAL",
                null,
                null,
                null,
                null,
                null
        );

        return new Scenario(ownerId, ownerEmail, targetStudentId, targetStudentEmail, groupId, sessionId);
    }

    private void replacePolicy(
            UUID groupId,
            UUID actorUserId,
            BigDecimal lateWeight,
            BigDecimal warningBelowRate,
            BigDecimal criticalBelowRate,
            Integer warningAbsentCount,
            Integer criticalAbsentCount
    ) {
        jdbcTemplate.update(
                "delete from attendance_policies where group_id = UUID_TO_BIN(?, 1)",
                groupId.toString()
        );

        jdbcTemplate.update("""
                insert into attendance_policies (
                    id,
                    group_id,
                    late_weight,
                    warning_below_rate,
                    critical_below_rate,
                    warning_absent_count,
                    critical_absent_count,
                    created_by_user_id,
                    updated_by_user_id,
                    created_at,
                    updated_at
                ) values (
                    UUID_TO_BIN(?, 1),
                    UUID_TO_BIN(?, 1),
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    UUID_TO_BIN(?, 1),
                    UUID_TO_BIN(?, 1),
                    UTC_TIMESTAMP(6),
                    UTC_TIMESTAMP(6)
                )
                """,
                UUID.randomUUID().toString(),
                groupId.toString(),
                lateWeight,
                warningBelowRate,
                criticalBelowRate,
                warningAbsentCount,
                criticalAbsentCount,
                actorUserId.toString(),
                actorUserId.toString()
        );
    }

    private UUID seedClosedSessionWithAttendance(Scenario s, String title, String attendanceStatus) {
        UUID sessionId = UUID.randomUUID();
        insertSession(sessionId, s.groupId(), s.ownerId(), "CLOSED", true);
        insertAttendance(sessionId, s.targetStudentId(), attendanceStatus, "MANUAL", null, null, null, null, null);
        return sessionId;
    }

    private void makeAttendancePresentQrLike(UUID sessionId, UUID userId, UUID actorUserId) {
        jdbcTemplate.update("""
                update session_attendance
                set attendance_status = 'PRESENT',
                    check_in_at = UTC_TIMESTAMP(6),
                    check_in_method = 'QR',
                    updated_at = UTC_TIMESTAMP(6)
                where session_id = UUID_TO_BIN(?, 1)
                  and user_id = UUID_TO_BIN(?, 1)
                """,
                sessionId.toString(),
                userId.toString()
        );
    }

    private long countPolicyNotifications(UUID recipientUserId, String type, UUID sourceSessionId) {
        Number n = jdbcTemplate.queryForObject("""
                select count(*)
                from notifications
                where recipient_user_id = UUID_TO_BIN(?, 1)
                  and type = ?
                  and source_type = 'ATTENDANCE_POLICY'
                  and source_ref_id = UUID_TO_BIN(?, 1)
                """,
                Number.class,
                recipientUserId.toString(),
                type,
                sourceSessionId.toString()
        );
        return n == null ? 0L : n.longValue();
    }

    private long countAnyPolicyNotifications(UUID recipientUserId) {
        Number n = jdbcTemplate.queryForObject("""
                select count(*)
                from notifications
                where recipient_user_id = UUID_TO_BIN(?, 1)
                  and type in (
                      'ATTENDANCE_POLICY_WARNING',
                      'ATTENDANCE_POLICY_CRITICAL',
                      'ATTENDANCE_POLICY_EXAM_BANNED'
                  )
                """,
                Number.class,
                recipientUserId.toString()
        );
        return n == null ? 0L : n.longValue();
    }

    private Map<String, Object> findPolicyNotificationRow(UUID recipientUserId, String type, UUID sourceSessionId) {
        return jdbcTemplate.queryForMap("""
                select
                    BIN_TO_UUID(id, 1) as id,
                    title,
                    body,
                    severity,
                    source_type,
                    BIN_TO_UUID(source_ref_id, 1) as source_ref_id,
                    dedup_key,
                    cast(payload_json as char) as payload_json
                from notifications
                where recipient_user_id = UUID_TO_BIN(?, 1)
                  and type = ?
                  and source_ref_id = UUID_TO_BIN(?, 1)
                order by created_at desc
                limit 1
                """,
                recipientUserId.toString(),
                type,
                sourceSessionId.toString()
        );
    }

    private RequestPostProcessor authAs(UUID userId, String email) {
        UserPrincipal principal = Mockito.mock(UserPrincipal.class);
        Mockito.when(principal.getUserId()).thenReturn(userId);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of()
        );

        return SecurityMockMvcRequestPostProcessors.authentication(authentication);
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
                """,
                id.toString(),
                email,
                email
        );
    }

    private void seedGroup(UUID groupId, UUID ownerUserId, String code, String name) {
        String safeCode = (code == null || code.isBlank()) ? "GRP001" : code;
        if (safeCode.length() > 20) {
            safeCode = safeCode.substring(0, 20);
        }

        String joinCode = "ABC12345";

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
    """,
                groupId.toString(),
                ownerUserId.toString(),
                safeCode,
                name,
                joinCode
        );
    }

    private void seedApprovedOwnerMembership(UUID groupId, UUID userId) {
        seedMembership(groupId, userId, "OWNER");
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
            DATE_SUB(UTC_TIMESTAMP(6), interval 30 day),
            DATE_SUB(UTC_TIMESTAMP(6), interval 30 day),
            UTC_TIMESTAMP(6)
        )
    """, groupId.toString(), userId.toString(), role);
    }

    private void insertSession(
            UUID sessionId,
            UUID groupId,
            UUID createdByUserId,
            String status,
            boolean allowManualOverride
    ) {
        if ("OPEN".equals(status)) {
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
                        ?,
                        15,
                        60,
                        30,
                        'test-secret',
                        'seeded by AttendanceAdminManualOverrideIT',
                        UUID_TO_BIN(?, 1),
                        UTC_TIMESTAMP(6),
                        UTC_TIMESTAMP(6),
                        null
                    )
                    """,
                    sessionId.toString(),
                    groupId.toString(),
                    "OPEN session",
                    allowManualOverride,
                    createdByUserId.toString()
            );
            return;
        }

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
                    ?,
                    15,
                    60,
                    30,
                    'test-secret-closed',
                    'seeded closed session by AttendanceAdminManualOverrideIT',
                    UUID_TO_BIN(?, 1),
                    UTC_TIMESTAMP(6),
                    UTC_TIMESTAMP(6),
                    null
                )
                """,
                sessionId.toString(),
                groupId.toString(),
                "CLOSED session",
                allowManualOverride,
                createdByUserId.toString()
        );
    }

    private void insertAttendance(
            UUID sessionId,
            UUID userId,
            String attendanceStatus,
            String checkInMethod,
            UUID qrTokenId,
            String deviceId,
            String ipAddress,
            String userAgent,
            UUID excusedByRequestId
    ) {
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
                    case when ? in ('QR', 'MANUAL') then UTC_TIMESTAMP(6) else null end,
                    ?,
                    null,
                    ?,
                    ?,
                    ?,
                    null,
                    null,
                    null,
                    false,
                    null,
                    UUID_TO_BIN(?, 1),
                    UTC_TIMESTAMP(6),
                    UTC_TIMESTAMP(6)
                )
                """,
                sessionId.toString(),
                userId.toString(),
                attendanceStatus,
                checkInMethod,
                checkInMethod,
                deviceId,
                ipAddress,
                userAgent,
                excusedByRequestId == null ? null : excusedByRequestId.toString()
        );
    }
}
