package com.attendance.backend.attendance.api;

import com.attendance.backend.security.UserPrincipal;
import com.attendance.backend.security.jwt.JwtAuthenticationFilter;
import com.attendance.backend.support.AbstractMySqlIntegrationTest;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(
        scripts = "/sql/cleanup/cleanup_all.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
        scripts = "/sql/cleanup/cleanup_all.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class AttendancePolicyStatusControllerIT extends AbstractMySqlIntegrationTest {

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final UUID TEST_GROUP_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TEST_OWNER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID TEST_MEMBER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeEach
    void setUp() throws Exception {
        allowJwtFilterToPassThrough();
        seedBaseline();
    }

    private void allowJwtFilterToPassThrough() throws Exception {
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    private void seedBaseline() {
        seedUser(TEST_OWNER_ID, "owner-policy@it.local");
        seedUser(TEST_MEMBER_ID, "member-policy@it.local");

        seedGroup(TEST_GROUP_ID, TEST_OWNER_ID, "POL01", "Policy IT Group", "ABC123");
        seedApprovedOwnerMembership(TEST_GROUP_ID, TEST_OWNER_ID);
        seedApprovedMemberMembership(TEST_GROUP_ID, TEST_MEMBER_ID);
    }

    @Test
    @DisplayName("me status trả NO_DATA khi không có CLOSED session")
    void me_status_returns_no_data_when_no_closed_sessions() throws Exception {
        mockMvc.perform(get("/api/v1/groups/{groupId}/me/attendance-policy-status", TEST_GROUP_ID)
                        .with(authUser(TEST_MEMBER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").value(TEST_GROUP_ID.toString()))
                .andExpect(jsonPath("$.eligibleSessionCount").value(0))
                .andExpect(jsonPath("$.attendanceRate").doesNotExist())
                .andExpect(jsonPath("$.policyStatus").value("NO_DATA"));
    }

    @Test
    @DisplayName("group list yêu cầu OWNER hoặc CO_HOST")
    void group_list_requires_owner_or_cohost() throws Exception {
        mockMvc.perform(get("/api/v1/groups/{groupId}/attendance-policy/students", TEST_GROUP_ID)
                        .with(authUser(TEST_MEMBER_ID)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("group list trả policy và items cho OWNER")
    void group_list_returns_effective_policy_and_items() throws Exception {
        mockMvc.perform(get("/api/v1/groups/{groupId}/attendance-policy/students", TEST_GROUP_ID)
                        .with(authUser(TEST_OWNER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policy").exists())
                .andExpect(jsonPath("$.policy.source").exists())
                .andExpect(jsonPath("$.policy.lateWeight").exists())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("EXCUSED không được tính vào denominator")
    void me_status_excludes_excused_from_denominator() throws Exception {
        updateMembershipJoinedAt(TEST_GROUP_ID, TEST_MEMBER_ID, Instant.parse("2000-01-01T00:00:00Z"));
        replacePolicy(
                TEST_GROUP_ID,
                new BigDecimal("0.50"),
                new BigDecimal("70.00"),
                new BigDecimal("40.00"),
                2,
                4
        );

        Instant firstStart = Instant.parse("2026-03-01T09:00:00Z");
        seedSequentialClosedSessionsWithAttendance(
                TEST_MEMBER_ID,
                firstStart,
                "PRESENT",
                "LATE",
                "ABSENT",
                "EXCUSED",
                "EXCUSED"
        );

        mockMvc.perform(get("/api/v1/groups/{groupId}/me/attendance-policy-status", TEST_GROUP_ID)
                        .with(authUser(TEST_MEMBER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closedSessionCount").value(5))
                .andExpect(jsonPath("$.eligibleSessionCount").value(3))
                .andExpect(jsonPath("$.presentCount").value(1))
                .andExpect(jsonPath("$.lateCount").value(1))
                .andExpect(jsonPath("$.absentCount").value(1))
                .andExpect(jsonPath("$.excusedCount").value(2))
                .andExpect(jsonPath("$.earnedAttendancePoints").value(1.50))
                .andExpect(jsonPath("$.attendanceRate").value(50.00));
    }

    @Test
    @DisplayName("thiếu session_attendance row thì vẫn bị tính là ABSENT")
    void me_status_counts_missing_session_attendance_row_as_absent() throws Exception {
        updateMembershipJoinedAt(TEST_GROUP_ID, TEST_MEMBER_ID, Instant.parse("2000-01-01T00:00:00Z"));
        replacePolicy(
                TEST_GROUP_ID,
                new BigDecimal("1.00"),
                new BigDecimal("60.00"),
                new BigDecimal("30.00"),
                2,
                4
        );

        UUID presentSessionId = seedSession(
                TEST_GROUP_ID,
                TEST_OWNER_ID,
                "Policy IT - present row exists",
                Instant.parse("2026-03-01T09:00:00Z"),
                "CLOSED",
                null
        );
        seedAttendance(presentSessionId, TEST_MEMBER_ID, "PRESENT");

        seedSession(
                TEST_GROUP_ID,
                TEST_OWNER_ID,
                "Policy IT - missing row becomes ABSENT",
                Instant.parse("2026-03-02T09:00:00Z"),
                "CLOSED",
                null
        );

        mockMvc.perform(get("/api/v1/groups/{groupId}/me/attendance-policy-status", TEST_GROUP_ID)
                        .with(authUser(TEST_MEMBER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closedSessionCount").value(2))
                .andExpect(jsonPath("$.eligibleSessionCount").value(2))
                .andExpect(jsonPath("$.presentCount").value(1))
                .andExpect(jsonPath("$.lateCount").value(0))
                .andExpect(jsonPath("$.absentCount").value(1))
                .andExpect(jsonPath("$.excusedCount").value(0))
                .andExpect(jsonPath("$.attendanceRate").value(50.00));
    }

    @Test
    @DisplayName("joinedAt cutoff loại các session trước khi membership có hiệu lực")
    void joined_at_cutoff_excludes_sessions_before_membership_effective_time() throws Exception {
        Instant joinedAt = Instant.parse("2026-03-01T00:00:00Z");
        updateMembershipJoinedAt(TEST_GROUP_ID, TEST_MEMBER_ID, joinedAt);
        replacePolicy(
                TEST_GROUP_ID,
                new BigDecimal("1.00"),
                new BigDecimal("60.00"),
                new BigDecimal("30.00"),
                2,
                4
        );

        UUID beforeJoinSessionId = seedSession(
                TEST_GROUP_ID,
                TEST_OWNER_ID,
                "Policy IT - before joinedAt",
                Instant.parse("2026-02-28T09:00:00Z"),
                "CLOSED",
                null
        );
        seedAttendance(beforeJoinSessionId, TEST_MEMBER_ID, "ABSENT");

        UUID afterJoinSessionId = seedSession(
                TEST_GROUP_ID,
                TEST_OWNER_ID,
                "Policy IT - after joinedAt",
                Instant.parse("2026-03-02T09:00:00Z"),
                "CLOSED",
                null
        );
        seedAttendance(afterJoinSessionId, TEST_MEMBER_ID, "PRESENT");

        mockMvc.perform(get("/api/v1/groups/{groupId}/me/attendance-policy-status", TEST_GROUP_ID)
                        .with(authUser(TEST_MEMBER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closedSessionCount").value(1))
                .andExpect(jsonPath("$.eligibleSessionCount").value(1))
                .andExpect(jsonPath("$.presentCount").value(1))
                .andExpect(jsonPath("$.lateCount").value(0))
                .andExpect(jsonPath("$.absentCount").value(0))
                .andExpect(jsonPath("$.excusedCount").value(0))
                .andExpect(jsonPath("$.attendanceRate").value(100.00))
                .andExpect(jsonPath("$.policyStatus").value("NORMAL"));
    }

    @Test
    @DisplayName("OPEN/CANCELLED/deleted CLOSED session đều bị loại khỏi thống kê")
    void open_cancelled_deleted_sessions_are_excluded() throws Exception {
        updateMembershipJoinedAt(TEST_GROUP_ID, TEST_MEMBER_ID, Instant.parse("2000-01-01T00:00:00Z"));
        replacePolicy(
                TEST_GROUP_ID,
                new BigDecimal("1.00"),
                new BigDecimal("60.00"),
                new BigDecimal("30.00"),
                2,
                4
        );

        UUID closedActiveSessionId = seedSession(
                TEST_GROUP_ID,
                TEST_OWNER_ID,
                "Policy IT - CLOSED active",
                Instant.parse("2026-03-01T09:00:00Z"),
                "CLOSED",
                null
        );
        seedAttendance(closedActiveSessionId, TEST_MEMBER_ID, "PRESENT");

        UUID openSessionId = seedSession(
                TEST_GROUP_ID,
                TEST_OWNER_ID,
                "Policy IT - OPEN ignored",
                Instant.parse("2026-03-02T09:00:00Z"),
                "OPEN",
                null
        );
        seedAttendance(openSessionId, TEST_MEMBER_ID, "ABSENT");

        UUID cancelledSessionId = seedSession(
                TEST_GROUP_ID,
                TEST_OWNER_ID,
                "Policy IT - CANCELLED ignored",
                Instant.parse("2026-03-03T09:00:00Z"),
                "CANCELLED",
                null
        );
        seedAttendance(cancelledSessionId, TEST_MEMBER_ID, "ABSENT");

        UUID deletedClosedSessionId = seedSession(
                TEST_GROUP_ID,
                TEST_OWNER_ID,
                "Policy IT - deleted CLOSED ignored",
                Instant.parse("2026-03-04T09:00:00Z"),
                "CLOSED",
                Instant.parse("2026-03-04T12:00:00Z")
        );
        seedAttendance(deletedClosedSessionId, TEST_MEMBER_ID, "ABSENT");

        mockMvc.perform(get("/api/v1/groups/{groupId}/me/attendance-policy-status", TEST_GROUP_ID)
                        .with(authUser(TEST_MEMBER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closedSessionCount").value(1))
                .andExpect(jsonPath("$.eligibleSessionCount").value(1))
                .andExpect(jsonPath("$.presentCount").value(1))
                .andExpect(jsonPath("$.lateCount").value(0))
                .andExpect(jsonPath("$.absentCount").value(0))
                .andExpect(jsonPath("$.excusedCount").value(0))
                .andExpect(jsonPath("$.attendanceRate").value(100.00));
    }

    @Test
    @DisplayName("policyStatus được suy ra đúng theo rate và absent count")
    void policy_status_warning_and_critical_are_derived_from_rate_and_absent_count() throws Exception {
        UUID rateWarningUserId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        UUID absentCriticalUserId = UUID.fromString("55555555-5555-5555-5555-555555555555");

        seedUser(rateWarningUserId, "warning-policy@it.local");
        seedUser(absentCriticalUserId, "critical-policy@it.local");
        seedApprovedMemberMembership(TEST_GROUP_ID, rateWarningUserId);
        seedApprovedMemberMembership(TEST_GROUP_ID, absentCriticalUserId);

        updateMembershipJoinedAt(TEST_GROUP_ID, rateWarningUserId, Instant.parse("2000-01-01T00:00:00Z"));
        updateMembershipJoinedAt(TEST_GROUP_ID, absentCriticalUserId, Instant.parse("2000-01-01T00:00:00Z"));

        replacePolicy(
                TEST_GROUP_ID,
                new BigDecimal("1.00"),
                new BigDecimal("80.00"),
                new BigDecimal("60.00"),
                10,
                20
        );

        seedSequentialClosedSessionsWithAttendance(
                rateWarningUserId,
                Instant.parse("2026-03-10T09:00:00Z"),
                "PRESENT",
                "PRESENT",
                "PRESENT",
                "ABSENT"
        );

        mockMvc.perform(get("/api/v1/groups/{groupId}/me/attendance-policy-status", TEST_GROUP_ID)
                        .with(authUser(rateWarningUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligibleSessionCount").value(4))
                .andExpect(jsonPath("$.presentCount").value(3))
                .andExpect(jsonPath("$.absentCount").value(1))
                .andExpect(jsonPath("$.attendanceRate").value(75.00))
                .andExpect(jsonPath("$.policyStatus").value("WARNING"))
                .andExpect(jsonPath("$.breachReasons", hasSize(1)))
                .andExpect(jsonPath("$.breachReasons", hasItem("RATE_BELOW_WARNING")));

        // Quan trọng: cắt membership của user critical sau 4 buổi đầu để isolate scenario này
        updateMembershipJoinedAt(TEST_GROUP_ID, absentCriticalUserId, Instant.parse("2026-03-20T00:00:00Z"));

        replacePolicy(
                TEST_GROUP_ID,
                new BigDecimal("1.00"),
                new BigDecimal("50.00"),
                new BigDecimal("30.00"),
                1,
                2
        );

        seedSequentialClosedSessionsWithAttendance(
                absentCriticalUserId,
                Instant.parse("2026-03-20T09:00:00Z"),
                "PRESENT",
                "PRESENT",
                "PRESENT",
                "PRESENT",
                "PRESENT",
                "ABSENT",
                "ABSENT"
        );

        mockMvc.perform(get("/api/v1/groups/{groupId}/me/attendance-policy-status", TEST_GROUP_ID)
                        .with(authUser(absentCriticalUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligibleSessionCount").value(7))
                .andExpect(jsonPath("$.presentCount").value(5))
                .andExpect(jsonPath("$.absentCount").value(2))
                .andExpect(jsonPath("$.attendanceRate").value(71.43))
                .andExpect(jsonPath("$.policyStatus").value("CRITICAL"))
                .andExpect(jsonPath("$.breachReasons", hasSize(1)))
                .andExpect(jsonPath("$.breachReasons", hasItem("ABSENT_COUNT_CRITICAL")));
    }

    private RequestPostProcessor authUser(UUID userId) {
        UserPrincipal principal = new UserPrincipal() {
            @Override
            public UUID getUserId() {
                return userId;
            }

            @Override
            public UUID getSessionId() {
                return UUID.fromString("90000000-0000-0000-0000-000000000001");
            }

            @Override
            public String getRole() {
                return "USER";
            }
        };

        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal,
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        return authentication(auth);
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

    private void seedGroup(UUID groupId, UUID ownerUserId, String code, String name, String joinCode) {
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

    private void updateMembershipJoinedAt(UUID groupId, UUID userId, Instant joinedAt) {
        jdbcTemplate.update("""
            update group_members
            set joined_at = ?,
                updated_at = UTC_TIMESTAMP(6)
            where group_id = UUID_TO_BIN(?, 1)
              and user_id = UUID_TO_BIN(?, 1)
        """, Timestamp.from(joinedAt), groupId.toString(), userId.toString());
    }

    private void replacePolicy(
            UUID groupId,
            BigDecimal lateWeight,
            BigDecimal warningBelowRate,
            BigDecimal criticalBelowRate,
            Integer warningAbsentCount,
            Integer criticalAbsentCount
    ) {
        jdbcTemplate.update("delete from attendance_policies where group_id = UUID_TO_BIN(?, 1)", groupId.toString());

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
                TEST_OWNER_ID.toString(),
                TEST_OWNER_ID.toString()
        );
    }

    private UUID seedSession(
            UUID groupId,
            UUID createdByUserId,
            String title,
            Instant startAt,
            String status,
            Instant deletedAt
    ) {
        UUID sessionId = UUID.randomUUID();
        Instant endAt = startAt.plus(2, ChronoUnit.HOURS);
        Instant checkinOpenAt = startAt.minus(15, ChronoUnit.MINUTES);
        Instant checkinCloseAt = startAt.plus(60, ChronoUnit.MINUTES);

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
                ?,
                ?,
                ?,
                ?,
                ?,
                ?,
                true,
                15,
                60,
                30,
                'policy-it-secret',
                'seeded by AttendancePolicyStatusControllerIT',
                UUID_TO_BIN(?, 1),
                UTC_TIMESTAMP(6),
                UTC_TIMESTAMP(6),
                ?
            )
        """,
                sessionId.toString(),
                groupId.toString(),
                title,
                Date.valueOf(startAt.atOffset(ZoneOffset.UTC).toLocalDate()),
                Timestamp.from(startAt),
                Timestamp.from(endAt),
                Timestamp.from(checkinOpenAt),
                Timestamp.from(checkinCloseAt),
                status,
                createdByUserId.toString(),
                deletedAt == null ? null : Timestamp.from(deletedAt)
        );

        return sessionId;
    }

    private void seedAttendance(UUID sessionId, UUID userId, String attendanceStatus) {
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
        """, sessionId.toString(), userId.toString(), attendanceStatus);
    }

    private void seedSequentialClosedSessionsWithAttendance(UUID userId, Instant firstStart, String... attendanceStatuses) {
        for (int i = 0; i < attendanceStatuses.length; i++) {
            UUID sessionId = seedSession(
                    TEST_GROUP_ID,
                    TEST_OWNER_ID,
                    "Policy IT session #" + (i + 1) + " for " + userId,
                    firstStart.plus(i, ChronoUnit.DAYS),
                    "CLOSED",
                    null
            );
            seedAttendance(sessionId, userId, attendanceStatuses[i]);
        }
    }
}
