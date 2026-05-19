package com.attendance.backend.auth.service;

import com.attendance.backend.auth.repository.LoginAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@Service
public class LoginAttemptService {

    public static final String OUTCOME_SUCCESS = "SUCCESS";
    public static final String OUTCOME_INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String OUTCOME_USER_NOT_ACTIVE = "USER_NOT_ACTIVE";
    public static final String OUTCOME_REQUIRE_PASSWORD_CHANGE = "REQUIRE_PASSWORD_CHANGE";
    public static final String OUTCOME_THROTTLED_IP = "THROTTLED_IP";
    public static final String OUTCOME_THROTTLED_EMAIL_IP = "THROTTLED_EMAIL_IP";

    private final LoginAttemptRepository repository;

    public LoginAttemptService(LoginAttemptRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(String emailNorm,
                       UUID userId,
                       String requestedIp,
                       String userAgent,
                       String outcome) {
        LoginAttempt row = new LoginAttempt();
        row.id = UUID.randomUUID();
        row.emailHash = sha256(emailNorm == null ? "" : emailNorm);
        row.userId = userId;
        row.requestedIp = requestedIp;
        row.userAgent = userAgent;
        row.outcome = outcome;
        repository.save(row);
    }

    private byte[] sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(raw.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to hash login email", ex);
        }
    }
}