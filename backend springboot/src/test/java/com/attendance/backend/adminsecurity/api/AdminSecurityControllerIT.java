package com.attendance.backend.adminsecurity.api;

import com.attendance.backend.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminSecurityControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        cleanSecurityTables();
    }

    @AfterEach
    void tearDown() {
        cleanSecurityTables();
    }

    @Test
    void overview_shouldReturnAggregatedSecurityMetrics() throws Exception {
        UUID adminUserId = UUID.randomUUID();
        seedUser(adminUserId, "ADMIN", "admin-security-it-overview@example.com");

        seedPasswordResetAttempt(adminUserId, "203.0.113.10", "ISSUED", 1);
        seedPasswordResetAttempt(adminUserId, "203.0.113.10", "THROTTLED_IP", 1);
        seedPasswordResetAttempt(adminUserId, "203.0.113.10", "MAIL_DELIVERY_FAILED", 1);
        seedPasswordResetAttempt(adminUserId, "203.0.113.10", "ISSUED", 72);

        seedLoginAttempt(adminUserId, "198.51.100.20", "SUCCESS", 1);
        seedLoginAttempt(adminUserId, "198.51.100.20", "INVALID_CREDENTIALS", 1);
        seedLoginAttempt(adminUserId, "198.51.100.20", "THROTTLED_IP", 1);
        seedLoginAttempt(adminUserId, "198.51.100.20", "SUCCESS", 72);

        seedEmailOutbox("pending-security-it@example.com", "PENDING", 0, null, null, 15, 5);
        seedEmailOutbox("retry-security-it@example.com", "RETRY", 2, "SMTP_TIMEOUT", "Retry later", 15, 4);
        seedEmailOutbox("dead-security-it@example.com", "DEAD", 5, "SMTP_DEAD", "Dead letter", 15, 3);

        seedRevokedSession(adminUserId, "PASSWORD_RESET", 1);
        seedRevokedSession(adminUserId, "PASSWORD_RESET", 72);

        mockMvc.perform(get("/api/v1/admin/security/overview")
                        .with(auth(adminUserId))
                        .queryParam("hours", "24")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.hours").value(24))
                .andExpect(jsonPath("$.passwordResetIssued").value(1))
                .andExpect(jsonPath("$.passwordResetThrottled").value(1))
                .andExpect(jsonPath("$.passwordResetMailFailed").value(1))
                .andExpect(jsonPath("$.loginSuccess").value(1))
                .andExpect(jsonPath("$.loginInvalidCredentials").value(1))
                .andExpect(jsonPath("$.loginThrottled").value(1))
                .andExpect(jsonPath("$.emailOutboxPending").value(1))
                .andExpect(jsonPath("$.emailOutboxRetry").value(1))
                .andExpect(jsonPath("$.emailOutboxDead").value(1))
                .andExpect(jsonPath("$.sessionsRevokedByPasswordReset").value(1));
    }

    @Test
    void passwordResetAbuse_shouldReturnTopSources() throws Exception {
        UUID adminUserId = UUID.randomUUID();
        seedUser(adminUserId, "ADMIN", "admin-security-it-password-reset@example.com");

        seedPasswordResetAttempt(adminUserId, "203.0.113.10", "THROTTLED_IP", 1);
        seedPasswordResetAttempt(adminUserId, "203.0.113.10", "THROTTLED_EMAIL", 1);
        seedPasswordResetAttempt(adminUserId, "203.0.113.10", "ISSUED", 1);

        seedPasswordResetAttempt(adminUserId, "203.0.113.20", "ISSUED", 1);
        seedPasswordResetAttempt(adminUserId, "203.0.113.20", "ISSUED", 72);

        mockMvc.perform(get("/api/v1/admin/security/password-reset-abuse")
                        .with(auth(adminUserId))
                        .queryParam("hours", "24")
                        .queryParam("limit", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].source").value("203.0.113.10"))
                .andExpect(jsonPath("$[0].totalCount").value(3))
                .andExpect(jsonPath("$[0].throttledCount").value(2))
                .andExpect(jsonPath("$[1].source").value("203.0.113.20"))
                .andExpect(jsonPath("$[1].totalCount").value(1))
                .andExpect(jsonPath("$[1].throttledCount").value(0));
    }

    @Test
    void loginAbuse_shouldReturnTopSources() throws Exception {
        UUID adminUserId = UUID.randomUUID();
        seedUser(adminUserId, "ADMIN", "admin-security-it-login@example.com");

        seedLoginAttempt(adminUserId, "198.51.100.10", "THROTTLED_IP", 1);
        seedLoginAttempt(adminUserId, "198.51.100.10", "INVALID_CREDENTIALS", 1);
        seedLoginAttempt(adminUserId, "198.51.100.10", "THROTTLED_EMAIL_IP", 1);

        seedLoginAttempt(adminUserId, "198.51.100.30", "SUCCESS", 1);
        seedLoginAttempt(adminUserId, "198.51.100.30", "SUCCESS", 72);

        mockMvc.perform(get("/api/v1/admin/security/login-abuse")
                        .with(auth(adminUserId))
                        .queryParam("hours", "24")
                        .queryParam("limit", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].source").value("198.51.100.10"))
                .andExpect(jsonPath("$[0].totalCount").value(3))
                .andExpect(jsonPath("$[0].throttledCount").value(2))
                .andExpect(jsonPath("$[1].source").value("198.51.100.30"))
                .andExpect(jsonPath("$[1].totalCount").value(1))
                .andExpect(jsonPath("$[1].throttledCount").value(0));
    }

    @Test
    void emailOutbox_shouldReturnRetryAndDeadItems_only() throws Exception {
        UUID adminUserId = UUID.randomUUID();
        seedUser(adminUserId, "ADMIN", "admin-security-it-outbox@example.com");

        UUID deadId = seedEmailOutbox("dead-security-it@example.com", "DEAD", 5, "SMTP_DEAD", "Dead letter", 10, 2);
        UUID retryId = seedEmailOutbox("retry-security-it@example.com", "RETRY", 2, "SMTP_TIMEOUT", "Retry later", 20, 1);
        seedEmailOutbox("pending-security-it@example.com", "PENDING", 0, null, null, 30, 3);

        mockMvc.perform(get("/api/v1/admin/security/email-outbox")
                        .with(auth(adminUserId))
                        .queryParam("limit", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(retryId.toString()))
                .andExpect(jsonPath("$[0].toEmail").value("retry-security-it@example.com"))
                .andExpect(jsonPath("$[0].status").value("RETRY"))
                .andExpect(jsonPath("$[0].retryCount").value(2))
                .andExpect(jsonPath("$[0].lastErrorCode").value("SMTP_TIMEOUT"))
                .andExpect(jsonPath("$[0].lastErrorMessage").value("Retry later"))
                .andExpect(jsonPath("$[0].nextAttemptAt", notNullValue()))
                .andExpect(jsonPath("$[0].createdAt", notNullValue()))
                .andExpect(jsonPath("$[1].id").value(deadId.toString()))
                .andExpect(jsonPath("$[1].toEmail").value("dead-security-it@example.com"))
                .andExpect(jsonPath("$[1].status").value("DEAD"))
                .andExpect(jsonPath("$[1].retryCount").value(5))
                .andExpect(jsonPath("$[1].lastErrorCode").value("SMTP_DEAD"))
                .andExpect(jsonPath("$[1].lastErrorMessage").value("Dead letter"))
                .andExpect(jsonPath("$[1].nextAttemptAt", notNullValue()))
                .andExpect(jsonPath("$[1].createdAt", notNullValue()));
    }

    @Test
    void overview_shouldReturnForbidden_forNonAdmin() throws Exception {
        UUID normalUserId = UUID.randomUUID();
        seedUser(normalUserId, "USER", "user-security-it-forbidden@example.com");

        mockMvc.perform(get("/api/v1/admin/security/overview")
                        .with(auth(normalUserId))
                        .queryParam("hours", "24")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    private RequestPostProcessor auth(UUID userId) {
        UserPrincipal principal = Mockito.mock(UserPrincipal.class);
        when(principal.getUserId()).thenReturn(userId);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        return authentication(authentication);
    }

    private void cleanSecurityTables() {
        jdbcTemplate.update("DELETE FROM user_sessions");
        jdbcTemplate.update("DELETE FROM password_reset_attempts");
        jdbcTemplate.update("DELETE FROM login_attempts");
        jdbcTemplate.update("DELETE FROM email_outbox");
        jdbcTemplate.update("DELETE FROM users WHERE email LIKE 'admin-security-it-%@example.com'");
        jdbcTemplate.update("DELETE FROM users WHERE email LIKE 'user-security-it-%@example.com'");
    }

    private void seedUser(UUID userId, String platformRole, String email) {
        jdbcTemplate.update("""
                INSERT INTO users (
                    id,
                    platform_role,
                    email,
                    password_hash,
                    full_name,
                    status,
                    created_at,
                    updated_at
                ) VALUES (
                    UUID_TO_BIN(?, 1),
                    ?,
                    ?,
                    ?,
                    ?,
                    'ACTIVE',
                    UTC_TIMESTAMP(3),
                    UTC_TIMESTAMP(3)
                )
                """,
                userId.toString(),
                platformRole,
                email,
                "$2a$10$abcdefghijklmnopqrstuvabcdefghijklmnopqrstuvabcd",
                "Admin Security IT"
        );
    }

    private void seedPasswordResetAttempt(UUID userId, String requestedIp, String outcome, int hoursAgo) {
        jdbcTemplate.update("""
                INSERT INTO password_reset_attempts (
                    id,
                    email_hash,
                    user_id,
                    requested_ip,
                    user_agent,
                    outcome,
                    created_at
                ) VALUES (
                    UUID_TO_BIN(?, 1),
                    ?,
                    UUID_TO_BIN(?, 1),
                    ?,
                    ?,
                    ?,
                    DATE_SUB(UTC_TIMESTAMP(3), INTERVAL ? HOUR)
                )
                """,
                UUID.randomUUID().toString(),
                bytes(32, 11),
                userId.toString(),
                requestedIp,
                "AdminSecurityControllerIT",
                outcome,
                hoursAgo
        );
    }

    private void seedLoginAttempt(UUID userId, String requestedIp, String outcome, int hoursAgo) {
        jdbcTemplate.update("""
                INSERT INTO login_attempts (
                    id,
                    email_hash,
                    user_id,
                    requested_ip,
                    user_agent,
                    outcome,
                    created_at
                ) VALUES (
                    UUID_TO_BIN(?, 1),
                    ?,
                    UUID_TO_BIN(?, 1),
                    ?,
                    ?,
                    ?,
                    DATE_SUB(UTC_TIMESTAMP(3), INTERVAL ? HOUR)
                )
                """,
                UUID.randomUUID().toString(),
                bytes(32, 22),
                userId.toString(),
                requestedIp,
                "AdminSecurityControllerIT",
                outcome,
                hoursAgo
        );
    }

    private UUID seedEmailOutbox(
            String toEmail,
            String status,
            int retryCount,
            String lastErrorCode,
            String lastErrorMessage,
            int nextAttemptInMinutes,
            int createdMinutesAgo
    ) {
        UUID id = UUID.randomUUID();

        jdbcTemplate.update("""
                INSERT INTO email_outbox (
                    id,
                    aggregate_type,
                    aggregate_id,
                    to_email,
                    subject,
                    payload_nonce,
                    payload_ciphertext,
                    status,
                    retry_count,
                    next_attempt_at,
                    locked_at,
                    processed_at,
                    last_error_code,
                    last_error_message,
                    created_at,
                    updated_at
                ) VALUES (
                    UUID_TO_BIN(?, 1),
                    ?,
                    NULL,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    DATE_ADD(UTC_TIMESTAMP(3), INTERVAL ? MINUTE),
                    NULL,
                    NULL,
                    ?,
                    ?,
                    DATE_SUB(UTC_TIMESTAMP(3), INTERVAL ? MINUTE),
                    DATE_SUB(UTC_TIMESTAMP(3), INTERVAL ? MINUTE)
                )
                """,
                id.toString(),
                "PASSWORD_RESET",
                toEmail,
                "Security Mail",
                bytes(12, 33),
                bytes(32, 44),
                status,
                retryCount,
                nextAttemptInMinutes,
                lastErrorCode,
                lastErrorMessage,
                createdMinutesAgo,
                createdMinutesAgo
        );

        return id;
    }

    private void seedRevokedSession(UUID userId, String revokedReason, int hoursAgo) {
        jdbcTemplate.update("""
                INSERT INTO user_sessions (
                    id,
                    user_id,
                    refresh_token_hash,
                    device_id,
                    ip_address,
                    user_agent,
                    issued_at,
                    expires_at,
                    last_used_at,
                    revoked_at,
                    revoked_reason,
                    created_at,
                    updated_at
                ) VALUES (
                    UUID_TO_BIN(?, 1),
                    UUID_TO_BIN(?, 1),
                    ?,
                    ?,
                    ?,
                    ?,
                    DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 10 DAY),
                    DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 10 DAY),
                    DATE_SUB(UTC_TIMESTAMP(3), INTERVAL ? HOUR),
                    DATE_SUB(UTC_TIMESTAMP(3), INTERVAL ? HOUR),
                    ?,
                    UTC_TIMESTAMP(3),
                    UTC_TIMESTAMP(3)
                )
                """,
                UUID.randomUUID().toString(),
                userId.toString(),
                uniqueBytes(32),
                "device-security-it",
                "192.0.2.10",
                "AdminSecurityControllerIT",
                hoursAgo,
                hoursAgo,
                revokedReason
        );
    }

    private byte[] bytes(int size, int seed) {
        byte[] value = new byte[size];
        for (int i = 0; i < size; i++) {
            value[i] = (byte) (seed + i);
        }
        return value;
    }

    private byte[] uniqueBytes(int size) {
        UUID uuid = UUID.randomUUID();
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        byte[] raw = buffer.array();

        byte[] value = new byte[size];
        for (int i = 0; i < size; i++) {
            value[i] = raw[i % raw.length];
        }
        return value;
    }
}