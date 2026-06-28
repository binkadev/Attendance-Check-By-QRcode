package com.attendance.backend.fraud.service;

import com.attendance.backend.attendance.service.AttendanceCheckinService;
import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.domain.enums.NotificationSourceType;
import com.attendance.backend.domain.enums.NotificationType;
import com.attendance.backend.notification.service.NotificationQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.within;

@SpringBootTest
@ActiveProfiles("test")
class QrCheckinFraudIntegrationIT {

    @MockitoBean
    private JavaMailSender javaMailSender;

    @Autowired
    private AttendanceCheckinService attendanceCheckinService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NotificationQueryService notificationQueryService;

    @Test
    void repeated_invalid_qr_token_should_create_single_incident_and_bump_same_row() {
        FraudScenario scenario = seedScenario(false);

        try {
            AttendanceCheckinService.QrCheckinCommand cmd =
                    new AttendanceCheckinService.QrCheckinCommand(
                            scenario.sessionId(),
                            scenario.studentId(),
                            "missing-token.secret-123",
                            "device-student-01",
                            "10.10.10.10",
                            "JUnit Invalid Token",
                            null,
                            null,
                            null
                    );

            for (int i = 0; i < 5; i++) {
                assertApiCode("QR_TOKEN_INVALID", () -> attendanceCheckinService.qrCheckin(cmd));
            }

            assertThat(countAttemptRows(scenario.sessionId(), scenario.studentId())).isEqualTo(5);
            assertThat(countAttemptRowsByFailureCode(
                    scenario.sessionId(),
                    scenario.studentId(),
                    "TOKEN_INVALID"
            )).isEqualTo(5);

            assertThat(countFraudIncidents(
                    scenario.sessionId(),
                    scenario.studentId(),
                    "REPEATED_FAILED_QR_TOKEN"
            )).isEqualTo(1);

            assertThat(readOccurrenceCount(
                    scenario.sessionId(),
                    scenario.studentId(),
                    "REPEATED_FAILED_QR_TOKEN"
            )).isEqualTo(1);

            assertThat(readIncidentSeverity(
                    scenario.sessionId(),
                    scenario.studentId(),
                    "REPEATED_FAILED_QR_TOKEN"
            )).isEqualTo("MEDIUM");

            assertThat(readIncidentStatus(
                    scenario.sessionId(),
                    scenario.studentId(),
                    "REPEATED_FAILED_QR_TOKEN"
            )).isEqualTo("OPEN");

            assertApiCode("QR_TOKEN_INVALID", () -> attendanceCheckinService.qrCheckin(cmd));

            assertThat(countAttemptRows(scenario.sessionId(), scenario.studentId())).isEqualTo(6);
            assertThat(countFraudIncidents(
                    scenario.sessionId(),
                    scenario.studentId(),
                    "REPEATED_FAILED_QR_TOKEN"
            )).isEqualTo(1);

            assertThat(readOccurrenceCount(
                    scenario.sessionId(),
                    scenario.studentId(),
                    "REPEATED_FAILED_QR_TOKEN"
            )).isEqualTo(2);

        } finally {
            cleanupScenario(scenario);
        }
    }

    @Test
    void repeated_wrong_session_token_should_create_single_high_incident_and_bump_same_row() {
        FraudScenario scenario = seedScenario(true);

        try {
            String tokenId = "token-" + UUID.randomUUID().toString().replace("-", "");
            String secret = "secret-wrong-session-123";
            seedQrToken(tokenId, scenario.otherSessionId(), secret, Instant.now().plusSeconds(600));

            AttendanceCheckinService.QrCheckinCommand cmd =
                    new AttendanceCheckinService.QrCheckinCommand(
                            scenario.sessionId(),
                            scenario.studentId(),
                            tokenId + "." + secret,
                            "device-student-02",
                            "10.20.30.40",
                            "JUnit Wrong Session",
                            null,
                            null,
                            null
                    );

            for (int i = 0; i < 3; i++) {
                assertApiCode("QR_TOKEN_NOT_FOR_SESSION", () -> attendanceCheckinService.qrCheckin(cmd));
            }

            assertThat(countAttemptRows(scenario.sessionId(), scenario.studentId())).isEqualTo(3);
            assertThat(countAttemptRowsByFailureCode(
                    scenario.sessionId(),
                    scenario.studentId(),
                    "TOKEN_WRONG_SESSION"
            )).isEqualTo(3);

            assertThat(countFraudIncidents(
                    scenario.sessionId(),
                    scenario.studentId(),
                    "WRONG_SESSION_QR_TOKEN"
            )).isEqualTo(1);

            assertThat(readOccurrenceCount(
                    scenario.sessionId(),
                    scenario.studentId(),
                    "WRONG_SESSION_QR_TOKEN"
            )).isEqualTo(1);

            assertThat(readIncidentSeverity(
                    scenario.sessionId(),
                    scenario.studentId(),
                    "WRONG_SESSION_QR_TOKEN"
            )).isEqualTo("HIGH");

            assertThat(readIncidentStatus(
                    scenario.sessionId(),
                    scenario.studentId(),
                    "WRONG_SESSION_QR_TOKEN"
            )).isEqualTo("OPEN");

            assertApiCode("QR_TOKEN_NOT_FOR_SESSION", () -> attendanceCheckinService.qrCheckin(cmd));

            assertThat(countAttemptRows(scenario.sessionId(), scenario.studentId())).isEqualTo(4);
            assertThat(countFraudIncidents(
                    scenario.sessionId(),
                    scenario.studentId(),
                    "WRONG_SESSION_QR_TOKEN"
            )).isEqualTo(1);

            assertThat(readOccurrenceCount(
                    scenario.sessionId(),
                    scenario.studentId(),
                    "WRONG_SESSION_QR_TOKEN"
            )).isEqualTo(2);

        } finally {
            cleanupScenario(scenario);
        }
    }

    @Test
    void shared_device_second_account_should_be_rejected_and_recorded_as_fraud_failure() {
        FraudScenario scenario = seedScenario(false);

        try {
            String tokenId = "token-" + UUID.randomUUID().toString().replace("-", "");
            String secret = "secret-shared-device-123";
            String deviceId = "shared-device-01";
            seedQrToken(tokenId, scenario.sessionId(), secret, Instant.now().plusSeconds(600));

            AttendanceCheckinService.QrCheckinCommand firstUserCmd =
                    new AttendanceCheckinService.QrCheckinCommand(
                            scenario.sessionId(),
                            scenario.studentId(),
                            tokenId + "." + secret,
                            deviceId,
                            "10.30.30.10",
                            "JUnit Shared Device A",
                            null,
                            null,
                            null
                    );

            AttendanceCheckinService.QrCheckinCommand secondUserCmd =
                    new AttendanceCheckinService.QrCheckinCommand(
                            scenario.sessionId(),
                            scenario.secondStudentId(),
                            tokenId + "." + secret,
                            deviceId,
                            "10.30.30.11",
                            "JUnit Shared Device B",
                            null,
                            null,
                            null
                    );

            AttendanceCheckinService.QrCheckinResult firstResult = attendanceCheckinService.qrCheckin(firstUserCmd);
            assertThat(firstResult.attendanceStatus().name()).isIn("PRESENT", "LATE");

            assertApiCode("SHARED_DEVICE_MULTI_ACCOUNT", () -> attendanceCheckinService.qrCheckin(secondUserCmd));

            assertThat(countPresentOrLateAttendanceRows(scenario.sessionId(), scenario.studentId())).isEqualTo(1);
            assertThat(countPresentOrLateAttendanceRows(scenario.sessionId(), scenario.secondStudentId())).isZero();
            assertThat(countCheckinQrEvents(scenario.sessionId(), scenario.studentId())).isEqualTo(1);
            assertThat(countCheckinQrEvents(scenario.sessionId(), scenario.secondStudentId())).isZero();
            assertThat(countCheckinSuccessNotifications(scenario.sessionId(), scenario.studentId())).isEqualTo(1);
            assertThat(countCheckinSuccessNotifications(scenario.sessionId(), scenario.secondStudentId())).isZero();

            assertThat(countAttemptRowsByFailureCode(
                    scenario.sessionId(),
                    scenario.secondStudentId(),
                    "SHARED_DEVICE_MULTI_ACCOUNT"
            )).isEqualTo(1);

            assertThat(countFraudIncidents(
                    scenario.sessionId(),
                    scenario.secondStudentId(),
                    "SHARED_DEVICE_MULTI_ACCOUNT"
            )).isEqualTo(1);

            assertThat(countFraudAlertNotifications(scenario.sessionId(), scenario.ownerId())).isEqualTo(1);
            assertThat(countFraudAlertNotifications(scenario.sessionId(), scenario.secondStudentId())).isZero();
            assertThat(readFraudAlertPayloadDeviceId(scenario.sessionId(), scenario.ownerId())).isEqualTo(deviceId);
            assertThat(notificationQueryService.getMyNotifications(
                    scenario.ownerId(),
                    0,
                    20,
                    false,
                    null,
                    null
            ).items()).anySatisfy(notification -> {
                assertThat(notification.type()).isEqualTo(NotificationType.FRAUD_ALERT_CRITICAL);
                assertThat(notification.sourceType()).isEqualTo(NotificationSourceType.FRAUD_INCIDENT);
            });

            AttendanceCheckinService.QrCheckinResult duplicateResult = attendanceCheckinService.qrCheckin(firstUserCmd);
            assertThat(duplicateResult.checkInAt())
                    .isCloseTo(firstResult.checkInAt(), within(2, ChronoUnit.MILLIS));
            assertThat(countCheckinQrEvents(scenario.sessionId(), scenario.studentId())).isEqualTo(1);

        } finally {
            cleanupScenario(scenario);
        }
    }

    private FraudScenario seedScenario(boolean withOtherSession) {
        UUID ownerId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID secondStudentId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID otherSessionId = withOtherSession ? UUID.randomUUID() : null;

        seedUser(ownerId, "owner-" + ownerId + "@it.local", "OWNER User", "USER");
        seedUser(studentId, "student-" + studentId + "@it.local", "Student User", "USER");
        seedUser(secondStudentId, "student2-" + secondStudentId + "@it.local", "Second Student User", "USER");

        seedGroup(groupId, ownerId, "GROUP-" + shortCode(groupId), "JOIN" + shortCode(groupId));
        seedMember(groupId, ownerId, "OWNER");
        seedMember(groupId, studentId, "MEMBER");
        seedMember(groupId, secondStudentId, "MEMBER");

        Instant now = Instant.now();
        seedSession(
                sessionId,
                groupId,
                ownerId,
                "OPEN",
                now.minusSeconds(60),
                now.plusSeconds(600)
        );

        if (withOtherSession) {
            seedSession(
                    otherSessionId,
                    groupId,
                    ownerId,
                    "CLOSED",
                    now.minusSeconds(600),
                    now.minusSeconds(300)
            );
        }

        return new FraudScenario(ownerId, studentId, secondStudentId, groupId, sessionId, otherSessionId);
    }

    private void seedUser(UUID userId, String email, String fullName, String platformRole) {
        jdbcTemplate.update(
                """
                insert into users (
                    id,
                    platform_role,
                    email,
                    password_hash,
                    full_name,
                    status,
                    created_at,
                    updated_at,
                    deleted_at
                ) values (
                    UUID_TO_BIN(?, 1),
                    ?,
                    ?,
                    ?,
                    ?,
                    'ACTIVE',
                    UTC_TIMESTAMP(3),
                    UTC_TIMESTAMP(3),
                    null
                )
                """,
                userId.toString(),
                platformRole,
                email,
                "$2a$10$dummyhashdummyhashdummyhashdummyhashdummyhashdummyha",
                fullName
        );
    }

    private void seedGroup(UUID groupId, UUID ownerUserId, String code, String joinCode) {
        jdbcTemplate.update(
                """
                insert into class_groups (
                    id,
                    owner_user_id,
                    name,
                    code,
                    join_code,
                    approval_mode,
                    allow_auto_join_on_checkin,
                    status,
                    created_at,
                    updated_at,
                    deleted_at
                ) values (
                    UUID_TO_BIN(?, 1),
                    UUID_TO_BIN(?, 1),
                    ?,
                    ?,
                    ?,
                    'AUTO',
                    0,
                    'ACTIVE',
                    UTC_TIMESTAMP(3),
                    UTC_TIMESTAMP(3),
                    null
                )
                """,
                groupId.toString(),
                ownerUserId.toString(),
                "Fraud Test Group " + code,
                code,
                joinCode
        );
    }

    private void seedMember(UUID groupId, UUID userId, String role) {
        jdbcTemplate.update(
                """
                insert into group_members (
                    group_id,
                    user_id,
                    role,
                    member_status,
                    joined_at,
                    created_at,
                    updated_at,
                    removed_at
                ) values (
                    UUID_TO_BIN(?, 1),
                    UUID_TO_BIN(?, 1),
                    ?,
                    'APPROVED',
                    UTC_TIMESTAMP(3),
                    UTC_TIMESTAMP(3),
                    UTC_TIMESTAMP(3),
                    null
                )
                """,
                groupId.toString(),
                userId.toString(),
                role
        );
    }

    private void seedSession(
            UUID sessionId,
            UUID groupId,
            UUID createdByUserId,
            String status,
            Instant openAt,
            Instant closeAt
    ) {
        jdbcTemplate.update(
                """
                insert into attendance_sessions (
                    id,
                    group_id,
                    created_by_user_id,
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
                    session_secret,
                    allow_manual_override,
                    note,
                    created_at,
                    updated_at,
                    deleted_at
                ) values (
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
                    15,
                    5,
                    15,
                    ?,
                    1,
                    ?,
                    UTC_TIMESTAMP(3),
                    UTC_TIMESTAMP(3),
                    null
                )
                """,
                sessionId.toString(),
                groupId.toString(),
                createdByUserId.toString(),
                "Fraud Session " + shortCode(sessionId),
                LocalDate.now(),
                status,
                Timestamp.from(openAt),
                Timestamp.from(closeAt),
                Timestamp.from(openAt),
                Timestamp.from(closeAt),
                "session-secret-" + shortCode(sessionId),
                "fraud-it"
        );
    }

    private void seedQrToken(String tokenId, UUID sessionId, String secret, Instant expiresAt) {
        jdbcTemplate.update(
                """
                insert into qr_tokens (
                    token_id,
                    session_id,
                    token_hash,
                    issued_at,
                    expires_at,
                    revoked_at,
                    revoked_reason,
                    rotated_from_token_id,
                    issued_by_user_id,
                    note
                ) values (
                    ?,
                    UUID_TO_BIN(?, 1),
                    ?,
                    UTC_TIMESTAMP(3),
                    ?,
                    null,
                    null,
                    null,
                    null,
                    ?
                )
                """,
                tokenId,
                sessionId.toString(),
                sha256(secret),
                Timestamp.from(expiresAt),
                "fraud-it-token"
        );
    }

    private void assertApiCode(String expectedCode, ThrowingRunnable runnable) {
        ApiException ex = catchThrowableOfType(runnable::run, ApiException.class);
        assertThat(ex).isNotNull();
        assertThat(ex.getCode()).isEqualTo(expectedCode);
    }

    private long countAttemptRows(UUID sessionId, UUID userId) {
        Long count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from checkin_attempt_logs
                where session_id = UUID_TO_BIN(?, 1)
                  and user_id = UUID_TO_BIN(?, 1)
                """,
                Long.class,
                sessionId.toString(),
                userId.toString()
        );
        return count == null ? 0L : count;
    }

    private long countAttemptRowsByFailureCode(UUID sessionId, UUID userId, String failureCode) {
        Long count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from checkin_attempt_logs
                where session_id = UUID_TO_BIN(?, 1)
                  and user_id = UUID_TO_BIN(?, 1)
                  and failure_code = ?
                """,
                Long.class,
                sessionId.toString(),
                userId.toString(),
                failureCode
        );
        return count == null ? 0L : count;
    }

    private long countFraudIncidents(UUID sessionId, UUID userId, String type) {
        Long count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from fraud_incidents
                where session_id = UUID_TO_BIN(?, 1)
                  and user_id = UUID_TO_BIN(?, 1)
                  and type = ?
                """,
                Long.class,
                sessionId.toString(),
                userId.toString(),
                type
        );
        return count == null ? 0L : count;
    }

    private int readOccurrenceCount(UUID sessionId, UUID userId, String type) {
        Integer value = jdbcTemplate.queryForObject(
                """
                select occurrence_count
                from fraud_incidents
                where session_id = UUID_TO_BIN(?, 1)
                  and user_id = UUID_TO_BIN(?, 1)
                  and type = ?
                limit 1
                """,
                Integer.class,
                sessionId.toString(),
                userId.toString(),
                type
        );
        return value == null ? 0 : value;
    }

    private String readIncidentSeverity(UUID sessionId, UUID userId, String type) {
        return jdbcTemplate.queryForObject(
                """
                select severity
                from fraud_incidents
                where session_id = UUID_TO_BIN(?, 1)
                  and user_id = UUID_TO_BIN(?, 1)
                  and type = ?
                limit 1
                """,
                String.class,
                sessionId.toString(),
                userId.toString(),
                type
        );
    }

    private String readIncidentStatus(UUID sessionId, UUID userId, String type) {
        return jdbcTemplate.queryForObject(
                """
                select status
                from fraud_incidents
                where session_id = UUID_TO_BIN(?, 1)
                  and user_id = UUID_TO_BIN(?, 1)
                  and type = ?
                limit 1
                """,
                String.class,
                sessionId.toString(),
                userId.toString(),
                type
        );
    }

    private long countPresentOrLateAttendanceRows(UUID sessionId, UUID userId) {
        Long count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from session_attendance
                where session_id = UUID_TO_BIN(?, 1)
                  and user_id = UUID_TO_BIN(?, 1)
                  and attendance_status in ('PRESENT', 'LATE')
                  and check_in_at is not null
                """,
                Long.class,
                sessionId.toString(),
                userId.toString()
        );
        return count == null ? 0L : count;
    }

    private long countCheckinQrEvents(UUID sessionId, UUID userId) {
        Long count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from attendance_events
                where session_id = UUID_TO_BIN(?, 1)
                  and user_id = UUID_TO_BIN(?, 1)
                  and event_type = 'CHECKIN_QR'
                """,
                Long.class,
                sessionId.toString(),
                userId.toString()
        );
        return count == null ? 0L : count;
    }

    private long countCheckinSuccessNotifications(UUID sessionId, UUID userId) {
        Long count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from notifications
                where session_id = UUID_TO_BIN(?, 1)
                  and recipient_user_id = UUID_TO_BIN(?, 1)
                  and type = 'CHECKIN_SUCCESS'
                """,
                Long.class,
                sessionId.toString(),
                userId.toString()
        );
        return count == null ? 0L : count;
    }

    private long countFraudAlertNotifications(UUID sessionId, UUID userId) {
        Long count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from notifications
                where session_id = UUID_TO_BIN(?, 1)
                  and recipient_user_id = UUID_TO_BIN(?, 1)
                  and type = 'FRAUD_ALERT_CRITICAL'
                  and severity = 'CRITICAL'
                  and source_type = 'FRAUD_INCIDENT'
                  and source_ref_id is not null
                """,
                Long.class,
                sessionId.toString(),
                userId.toString()
        );
        return count == null ? 0L : count;
    }

    private String readFraudAlertPayloadDeviceId(UUID sessionId, UUID userId) {
        return jdbcTemplate.queryForObject(
                """
                select json_unquote(json_extract(payload_json, '$.deviceId'))
                from notifications
                where session_id = UUID_TO_BIN(?, 1)
                  and recipient_user_id = UUID_TO_BIN(?, 1)
                  and type = 'FRAUD_ALERT_CRITICAL'
                limit 1
                """,
                String.class,
                sessionId.toString(),
                userId.toString()
        );
    }

    private void cleanupScenario(FraudScenario scenario) {
        jdbcTemplate.update(
                """
                delete nd
                from notification_deliveries nd
                join notifications n on n.id = nd.notification_id
                where n.session_id = UUID_TO_BIN(?, 1)
                   or n.session_id = UUID_TO_BIN(?, 1)
                """,
                scenario.sessionId().toString(),
                scenario.otherSessionId() == null ? scenario.sessionId().toString() : scenario.otherSessionId().toString()
        );

        jdbcTemplate.update(
                "delete from notifications where session_id = UUID_TO_BIN(?, 1) or session_id = UUID_TO_BIN(?, 1)",
                scenario.sessionId().toString(),
                scenario.otherSessionId() == null ? scenario.sessionId().toString() : scenario.otherSessionId().toString()
        );

        jdbcTemplate.update(
                "delete from fraud_incidents where session_id = UUID_TO_BIN(?, 1) or session_id = UUID_TO_BIN(?, 1)",
                scenario.sessionId().toString(),
                scenario.otherSessionId() == null ? scenario.sessionId().toString() : scenario.otherSessionId().toString()
        );

        jdbcTemplate.update(
                "delete from checkin_attempt_logs where session_id = UUID_TO_BIN(?, 1) or session_id = UUID_TO_BIN(?, 1)",
                scenario.sessionId().toString(),
                scenario.otherSessionId() == null ? scenario.sessionId().toString() : scenario.otherSessionId().toString()
        );

        jdbcTemplate.update(
                "delete from attendance_events where session_id = UUID_TO_BIN(?, 1) or session_id = UUID_TO_BIN(?, 1)",
                scenario.sessionId().toString(),
                scenario.otherSessionId() == null ? scenario.sessionId().toString() : scenario.otherSessionId().toString()
        );

        jdbcTemplate.update(
                "delete from session_attendance where session_id = UUID_TO_BIN(?, 1) or session_id = UUID_TO_BIN(?, 1)",
                scenario.sessionId().toString(),
                scenario.otherSessionId() == null ? scenario.sessionId().toString() : scenario.otherSessionId().toString()
        );

        jdbcTemplate.update(
                "delete from qr_tokens where session_id = UUID_TO_BIN(?, 1) or session_id = UUID_TO_BIN(?, 1)",
                scenario.sessionId().toString(),
                scenario.otherSessionId() == null ? scenario.sessionId().toString() : scenario.otherSessionId().toString()
        );

        jdbcTemplate.update(
                "delete from attendance_sessions where id = UUID_TO_BIN(?, 1)",
                scenario.sessionId().toString()
        );

        if (scenario.otherSessionId() != null) {
            jdbcTemplate.update(
                    "delete from attendance_sessions where id = UUID_TO_BIN(?, 1)",
                    scenario.otherSessionId().toString()
            );
        }

        jdbcTemplate.update(
                "delete from group_members where group_id = UUID_TO_BIN(?, 1)",
                scenario.groupId().toString()
        );

        jdbcTemplate.update(
                "delete from class_groups where id = UUID_TO_BIN(?, 1)",
                scenario.groupId().toString()
        );

        jdbcTemplate.update(
                "delete from users where id in (UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1), UUID_TO_BIN(?, 1))",
                scenario.ownerId().toString(),
                scenario.studentId().toString(),
                scenario.secondStudentId().toString()
        );
    }

    private byte[] sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash test token", ex);
        }
    }

    private String shortCode(UUID value) {
        return value.toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    @FunctionalInterface
    interface ThrowingRunnable {
        void run();
    }

    private record FraudScenario(
            UUID ownerId,
            UUID studentId,
            UUID secondStudentId,
            UUID groupId,
            UUID sessionId,
            UUID otherSessionId
    ) {
    }
}
