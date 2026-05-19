package com.attendance.backend.auth.service;

import com.attendance.backend.auth.config.PasswordResetProperties;
import com.attendance.backend.auth.dto.AuthDtos;
import com.attendance.backend.auth.dto.ChangePasswordRequest;
import com.attendance.backend.auth.dto.ForceChangePasswordRequest;
import com.attendance.backend.auth.dto.RegisterRequest;
import com.attendance.backend.auth.dto.RegisterResponse;
import com.attendance.backend.auth.dto.UpdateMeRequest;
import com.attendance.backend.auth.repository.PasswordResetTokenRepository;
import com.attendance.backend.auth.repository.UserRepository;
import com.attendance.backend.auth.repository.UserSessionRepository;
import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.domain.entity.PasswordResetToken;
import com.attendance.backend.domain.entity.User;
import com.attendance.backend.domain.entity.UserSession;
import com.attendance.backend.domain.enums.PlatformRole;
import com.attendance.backend.domain.enums.UserStatus;
import com.attendance.backend.mail.EmailOutboxService;
import com.attendance.backend.security.jwt.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final Logger securityAuditLog = LoggerFactory.getLogger("SECURITY_AUDIT");

    private static final String TOKEN_TYPE_BEARER = "Bearer";

    private static final String REVOKE_REASON_LOGOUT = "LOGOUT";
    private static final String REVOKE_REASON_LOGOUT_ALL = "LOGOUT_ALL";
    private static final String REVOKE_REASON_PASSWORD_CHANGED = "PASSWORD_CHANGED";
    private static final String REVOKE_REASON_PASSWORD_RESET = "PASSWORD_RESET";

    private static final String RESET_TOKEN_REVOKE_REASON_SUPERSEDED = "SUPERSEDED";
    private static final String RESET_TOKEN_REVOKE_REASON_COMPLETED = "PASSWORD_RESET_COMPLETED";
    private static final String RESET_TOKEN_REVOKE_REASON_MAIL_DELIVERY_FAILED = "MAIL_DELIVERY_FAILED";

    private static final String FORGOT_PASSWORD_NEUTRAL_MESSAGE =
            "If the email exists, a password reset link has been sent.";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordResetAttemptService passwordResetAttemptService;
    private final PasswordResetRedisRateLimitService passwordResetRedisRateLimitService;
    private final LoginRedisRateLimitService loginRedisRateLimitService;
    private final LoginAttemptService loginAttemptService;
    private final PasswordPolicyService passwordPolicyService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PasswordResetProperties passwordResetProperties;
    private final EmailOutboxService emailOutboxService;
    private final Clock clock;

    public AuthService(UserRepository userRepository,
                       UserSessionRepository userSessionRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       PasswordResetAttemptService passwordResetAttemptService,
                       PasswordResetRedisRateLimitService passwordResetRedisRateLimitService,
                       LoginRedisRateLimitService loginRedisRateLimitService,
                       LoginAttemptService loginAttemptService,
                       PasswordPolicyService passwordPolicyService,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       PasswordResetProperties passwordResetProperties,
                       EmailOutboxService emailOutboxService,
                       Clock clock) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordResetAttemptService = passwordResetAttemptService;
        this.passwordResetRedisRateLimitService = passwordResetRedisRateLimitService;
        this.loginRedisRateLimitService = loginRedisRateLimitService;
        this.loginAttemptService = loginAttemptService;
        this.passwordPolicyService = passwordPolicyService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.passwordResetProperties = passwordResetProperties;
        this.emailOutboxService = emailOutboxService;
        this.clock = clock;
    }

    @Transactional
    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest req, String ipAddress, String userAgent) {
        String emailNorm = normalizeEmail(req.email());
        String normalizedIp = normalizeNullable(ipAddress);
        String normalizedUserAgent = truncateUserAgent(userAgent);

        LoginRedisRateLimitService.Decision preDecision =
                loginRedisRateLimitService.checkBlocked(emailNorm, normalizedIp);

        if (!preDecision.allowed()) {
            loginAttemptService.record(
                    emailNorm,
                    null,
                    normalizedIp,
                    normalizedUserAgent,
                    preDecision.reason()
            );
            throw ApiException.tooManyRequests(
                    "LOGIN_RATE_LIMITED",
                    "Too many login attempts. Please try again later."
            );
        }

        User user = userRepository.findForLogin(emailNorm).orElse(null);

        if (user == null) {
            handleFailedLogin(
                    emailNorm,
                    null,
                    normalizedIp,
                    normalizedUserAgent,
                    LoginAttemptService.OUTCOME_INVALID_CREDENTIALS
            );
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            handleFailedLogin(
                    emailNorm,
                    user.getId(),
                    normalizedIp,
                    normalizedUserAgent,
                    LoginAttemptService.OUTCOME_USER_NOT_ACTIVE
            );
        }

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            handleFailedLogin(
                    emailNorm,
                    user.getId(),
                    normalizedIp,
                    normalizedUserAgent,
                    LoginAttemptService.OUTCOME_INVALID_CREDENTIALS
            );
        }

        if (user.isRequirePasswordChange()) {
            loginRedisRateLimitService.clearOnSuccess(emailNorm, normalizedIp);
            loginAttemptService.record(
                    emailNorm,
                    user.getId(),
                    normalizedIp,
                    normalizedUserAgent,
                    LoginAttemptService.OUTCOME_REQUIRE_PASSWORD_CHANGE
            );
            throw ApiException.preconditionRequired(
                    "REQUIRE_PASSWORD_CHANGE",
                    "You must change the default password before continuing."
            );
        }

        loginRedisRateLimitService.clearOnSuccess(emailNorm, normalizedIp);
        loginAttemptService.record(
                emailNorm,
                user.getId(),
                normalizedIp,
                normalizedUserAgent,
                LoginAttemptService.OUTCOME_SUCCESS
        );

        Instant now = Instant.now(clock);
        UUID sessionId = UUID.randomUUID();

        JwtService.TokenBundle tokenBundle = jwtService.issueTokenBundle(user, sessionId, now);

        UserSession session = new UserSession();
        session.setId(sessionId);
        session.setUserId(user.getId());
        session.setRefreshTokenHash(jwtService.hashRefreshToken(tokenBundle.refreshToken()));
        session.setDeviceId(normalizeNullable(req.deviceId()));
        session.setIpAddress(normalizedIp);
        session.setUserAgent(normalizedUserAgent);
        session.setIssuedAt(now);
        session.setExpiresAt(tokenBundle.refreshTokenExpiresAt());

        userSessionRepository.save(session);

        return toLoginResponse(user, tokenBundle);
    }

    private void handleFailedLogin(String emailNorm,
                                   UUID userId,
                                   String normalizedIp,
                                   String normalizedUserAgent,
                                   String baseOutcome) {
        LoginRedisRateLimitService.Decision failureDecision =
                loginRedisRateLimitService.recordFailure(emailNorm, normalizedIp);

        String finalOutcome = failureDecision.allowed() ? baseOutcome : failureDecision.reason();

        loginAttemptService.record(
                emailNorm,
                userId,
                normalizedIp,
                normalizedUserAgent,
                finalOutcome
        );

        if (!failureDecision.allowed()) {
            throw ApiException.tooManyRequests(
                    "LOGIN_RATE_LIMITED",
                    "Too many login attempts. Please try again later."
            );
        }

        if (LoginAttemptService.OUTCOME_USER_NOT_ACTIVE.equals(baseOutcome)) {
            throw ApiException.unauthorized("USER_DISABLED", "User is not active");
        }

        throw ApiException.unauthorized("INVALID_CREDENTIALS", "Invalid email or password");
    }

    @Transactional
    public RegisterResponse register(RegisterRequest req, String ipAddress, String userAgent) {
        try {
            String emailNorm = normalizeEmail(req.email());
            String fullName = normalizeText(req.fullName());
            String userCode = normalizeNullable(req.userCode());
            String deviceId = normalizeNullable(req.deviceId());

            if (!StringUtils.hasText(emailNorm)) {
                throw ApiException.badRequest("EMAIL_REQUIRED", "email is required");
            }

            passwordPolicyService.validateOrThrow(req.password());

            if (!StringUtils.hasText(fullName) || fullName.length() < 2) {
                throw ApiException.badRequest("FULL_NAME_INVALID", "fullName must be at least 2 characters");
            }

            if (userRepository.existsByEmailNorm(emailNorm)) {
                throw ApiException.conflict("EMAIL_ALREADY_EXISTS", "Email already exists");
            }

            if (StringUtils.hasText(userCode) && userRepository.existsByUserCode(userCode)) {
                throw ApiException.conflict("USER_CODE_ALREADY_EXISTS", "User code already exists");
            }

            User user = new User();
            user.setId(UUID.randomUUID());
            user.setEmail(emailNorm);
            user.setPasswordHash(passwordEncoder.encode(req.password()));
            user.setFullName(fullName);
            user.setUserCode(userCode);
            user.setPrimaryDeviceId(deviceId);
            user.setPlatformRole(PlatformRole.USER);
            user.setStatus(UserStatus.ACTIVE);
            user.setRequirePasswordChange(false);

            userRepository.saveAndFlush(user);

            Instant now = Instant.now(clock);
            UUID sessionId = UUID.randomUUID();

            JwtService.TokenBundle tokenBundle = jwtService.issueTokenBundle(user, sessionId, now);

            UserSession session = new UserSession();
            session.setId(sessionId);
            session.setUserId(user.getId());
            session.setRefreshTokenHash(jwtService.hashRefreshToken(tokenBundle.refreshToken()));
            session.setDeviceId(deviceId);
            session.setIpAddress(normalizeNullable(ipAddress));
            session.setUserAgent(truncateUserAgent(userAgent));
            session.setIssuedAt(now);
            session.setExpiresAt(tokenBundle.refreshTokenExpiresAt());

            userSessionRepository.save(session);

            return new RegisterResponse(
                    TOKEN_TYPE_BEARER,
                    tokenBundle.accessToken(),
                    tokenBundle.accessTokenExpiresAt(),
                    tokenBundle.refreshToken(),
                    tokenBundle.refreshTokenExpiresAt(),
                    tokenBundle.sessionId(),
                    new RegisterResponse.UserSummary(
                            user.getId(),
                            user.getEmail(),
                            user.getFullName(),
                            resolvePlatformRole(user).name()
                    ),
                    true
            );
        } catch (DataIntegrityViolationException ex) {
            throw mapRegisterConflict(ex);
        }
    }


    @Transactional
    public AuthDtos.LoginResponse forceChangePassword(ForceChangePasswordRequest req, String ipAddress, String userAgent) {
        String emailNorm = normalizeEmail(req.email());
        String normalizedIp = normalizeNullable(ipAddress);
        String normalizedUserAgent = truncateUserAgent(userAgent);

        if (!StringUtils.hasText(emailNorm)) {
            throw ApiException.badRequest("EMAIL_REQUIRED", "email is required");
        }

        User user = userRepository.findForLogin(emailNorm)
                .orElseThrow(() -> ApiException.unauthorized("INVALID_CREDENTIALS", "Invalid email or password"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw ApiException.unauthorized("USER_DISABLED", "User is not active");
        }

        if (!user.isRequirePasswordChange()) {
            throw ApiException.conflict("PASSWORD_CHANGE_NOT_REQUIRED", "Password change is not required for this account");
        }

        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            loginAttemptService.record(
                    emailNorm,
                    user.getId(),
                    normalizedIp,
                    normalizedUserAgent,
                    LoginAttemptService.OUTCOME_INVALID_CREDENTIALS
            );
            throw ApiException.unauthorized("INVALID_CREDENTIALS", "Invalid email or password");
        }

        if (req.currentPassword().equals(req.newPassword())) {
            throw ApiException.unprocessable(
                    "NEW_PASSWORD_MUST_BE_DIFFERENT",
                    "New password must be different from current password"
            );
        }

        passwordPolicyService.validateOrThrow(req.newPassword());

        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        user.setRequirePasswordChange(false);
        userRepository.save(user);

        Instant now = Instant.now(clock);
        userSessionRepository.revokeAllActiveByUserId(user.getId(), now, REVOKE_REASON_PASSWORD_CHANGED);

        UUID sessionId = UUID.randomUUID();
        JwtService.TokenBundle tokenBundle = jwtService.issueTokenBundle(user, sessionId, now);

        UserSession session = new UserSession();
        session.setId(sessionId);
        session.setUserId(user.getId());
        session.setRefreshTokenHash(jwtService.hashRefreshToken(tokenBundle.refreshToken()));
        session.setDeviceId(normalizeNullable(req.deviceId()));
        session.setIpAddress(normalizedIp);
        session.setUserAgent(normalizedUserAgent);
        session.setIssuedAt(now);
        session.setExpiresAt(tokenBundle.refreshTokenExpiresAt());

        userSessionRepository.save(session);

        loginRedisRateLimitService.clearOnSuccess(emailNorm, normalizedIp);
        loginAttemptService.record(
                emailNorm,
                user.getId(),
                normalizedIp,
                normalizedUserAgent,
                LoginAttemptService.OUTCOME_SUCCESS
        );

        return toLoginResponse(user, tokenBundle);
    }

    @Transactional
    public AuthDtos.LoginResponse refresh(AuthDtos.RefreshRequest req) {
        String rawRefreshToken = req.refreshToken();
        if (!StringUtils.hasText(rawRefreshToken)) {
            throw ApiException.badRequest("REFRESH_TOKEN_REQUIRED", "refreshToken is required");
        }

        JwtService.ParsedJwt parsed;
        try {
            parsed = jwtService.parseAndValidate(rawRefreshToken);
        } catch (Exception ex) {
            throw ApiException.unauthorized("INVALID_REFRESH_TOKEN", "Invalid refresh token");
        }

        if (!JwtService.TOKEN_TYPE_REFRESH.equals(parsed.type())) {
            throw ApiException.unauthorized("INVALID_REFRESH_TOKEN", "Invalid refresh token");
        }

        Instant now = Instant.now(clock);

        UserSession session = userSessionRepository.findByIdForUpdate(parsed.sessionId())
                .orElseThrow(() -> ApiException.unauthorized("INVALID_REFRESH_TOKEN", "Invalid refresh token"));

        if (session.isRevoked() || session.isExpiredAt(now)) {
            throw ApiException.unauthorized("SESSION_REVOKED", "Session is no longer active");
        }

        if (!session.getUserId().equals(parsed.userId())) {
            throw ApiException.unauthorized("INVALID_REFRESH_TOKEN", "Invalid refresh token");
        }

        if (!jwtService.matchesRefreshTokenHash(rawRefreshToken, session.getRefreshTokenHash())) {
            throw ApiException.unauthorized("INVALID_REFRESH_TOKEN", "Invalid refresh token");
        }

        User user = getCurrentActiveUserOrThrow(parsed.userId());

        JwtService.TokenBundle tokenBundle = jwtService.issueTokenBundle(user, session.getId(), now);

        session.setRefreshTokenHash(jwtService.hashRefreshToken(tokenBundle.refreshToken()));
        session.setIssuedAt(now);
        session.setExpiresAt(tokenBundle.refreshTokenExpiresAt());
        session.setLastUsedAt(now);

        userSessionRepository.save(session);

        return toLoginResponse(user, tokenBundle);
    }

    @Transactional
    public void logout(AuthDtos.LogoutRequest req) {
        String rawRefreshToken = req.refreshToken();
        if (!StringUtils.hasText(rawRefreshToken)) {
            throw ApiException.badRequest("REFRESH_TOKEN_REQUIRED", "refreshToken is required");
        }

        JwtService.ParsedJwt parsed;
        try {
            parsed = jwtService.parseAndValidate(rawRefreshToken);
        } catch (Exception ex) {
            return;
        }

        if (!JwtService.TOKEN_TYPE_REFRESH.equals(parsed.type())) {
            return;
        }

        Instant now = Instant.now(clock);

        userSessionRepository.findByIdForUpdate(parsed.sessionId())
                .ifPresent(session -> {
                    if (!session.isRevoked()) {
                        session.revoke(now, REVOKE_REASON_LOGOUT);
                        userSessionRepository.save(session);
                    }
                });
    }

    @Transactional
    public AuthDtos.ForgotPasswordResponse forgotPassword(AuthDtos.ForgotPasswordRequest req,
                                                          String ipAddress,
                                                          String userAgent) {
        String emailNorm = normalizeEmail(req.email());
        if (!StringUtils.hasText(emailNorm)) {
            throw ApiException.badRequest("EMAIL_REQUIRED", "email is required");
        }

        String normalizedIp = normalizeNullable(ipAddress);
        String normalizedUserAgent = truncateUserAgent(userAgent);

        PasswordResetRedisRateLimitService.Decision rateDecision =
                passwordResetRedisRateLimitService.evaluate(emailNorm, normalizedIp);

        User user = userRepository.findByEmailNorm(emailNorm).orElse(null);
        boolean userExists = user != null;
        boolean userActive = userExists && user.getStatus() == UserStatus.ACTIVE;

        if (!rateDecision.allowed()) {
            passwordResetAttemptService.record(
                    emailNorm,
                    userExists ? user.getId() : null,
                    normalizedIp,
                    normalizedUserAgent,
                    rateDecision.reason()
            );
            auditForgotPassword(emailNorm, userExists ? user.getId() : null, normalizedIp, rateDecision.reason());
            return new AuthDtos.ForgotPasswordResponse(FORGOT_PASSWORD_NEUTRAL_MESSAGE);
        }

        if (rateDecision.degraded()) {
            auditForgotPassword(emailNorm, userExists ? user.getId() : null, normalizedIp, rateDecision.reason());
        }

        if (!userExists) {
            passwordResetAttemptService.record(
                    emailNorm,
                    null,
                    normalizedIp,
                    normalizedUserAgent,
                    PasswordResetAttemptService.OUTCOME_EMAIL_NOT_FOUND
            );
            auditForgotPassword(emailNorm, null, normalizedIp, PasswordResetAttemptService.OUTCOME_EMAIL_NOT_FOUND);
            return new AuthDtos.ForgotPasswordResponse(FORGOT_PASSWORD_NEUTRAL_MESSAGE);
        }

        if (!userActive) {
            passwordResetAttemptService.record(
                    emailNorm,
                    user.getId(),
                    normalizedIp,
                    normalizedUserAgent,
                    PasswordResetAttemptService.OUTCOME_USER_NOT_ACTIVE
            );
            auditForgotPassword(emailNorm, user.getId(), normalizedIp, PasswordResetAttemptService.OUTCOME_USER_NOT_ACTIVE);
            return new AuthDtos.ForgotPasswordResponse(FORGOT_PASSWORD_NEUTRAL_MESSAGE);
        }

        Instant now = Instant.now(clock);
        String plainToken = generateOpaqueToken();
        byte[] tokenHash = sha256(plainToken);

        passwordResetTokenRepository.revokeAllActiveByUserId(
                user.getId(),
                now,
                RESET_TOKEN_REVOKE_REASON_SUPERSEDED
        );

        PasswordResetToken token = new PasswordResetToken();
        token.setId(UUID.randomUUID());
        token.setUserId(user.getId());
        token.setTokenHash(tokenHash);
        token.setRequestedIp(normalizedIp);
        token.setUserAgent(normalizedUserAgent);
        token.setExpiresAt(now.plus(passwordResetProperties.getTokenMinutes(), ChronoUnit.MINUTES));

        passwordResetTokenRepository.saveAndFlush(token);

        String resetUrl = buildResetUrl(passwordResetProperties.getFrontendResetUrl(), plainToken);

        try {
            emailOutboxService.enqueuePasswordResetEmail(
                    token.getId(),
                    user.getEmail(),
                    user.getFullName(),
                    resetUrl,
                    token.getExpiresAt()
            );

            passwordResetAttemptService.record(
                    emailNorm,
                    user.getId(),
                    normalizedIp,
                    normalizedUserAgent,
                    PasswordResetAttemptService.OUTCOME_ISSUED
            );
            auditForgotPassword(emailNorm, user.getId(), normalizedIp, PasswordResetAttemptService.OUTCOME_ISSUED);
        } catch (Exception ex) {
            token.revoke(now, RESET_TOKEN_REVOKE_REASON_MAIL_DELIVERY_FAILED);
            passwordResetTokenRepository.saveAndFlush(token);

            passwordResetAttemptService.record(
                    emailNorm,
                    user.getId(),
                    normalizedIp,
                    normalizedUserAgent,
                    PasswordResetAttemptService.OUTCOME_MAIL_DELIVERY_FAILED
            );
            auditForgotPassword(emailNorm, user.getId(), normalizedIp, PasswordResetAttemptService.OUTCOME_MAIL_DELIVERY_FAILED);
        }

        return new AuthDtos.ForgotPasswordResponse(FORGOT_PASSWORD_NEUTRAL_MESSAGE);
    }

    @Transactional
    public void resetPassword(AuthDtos.ResetPasswordRequest req) {
        String rawToken = req.token();
        if (!StringUtils.hasText(rawToken)) {
            throw ApiException.badRequest("RESET_TOKEN_REQUIRED", "token is required");
        }

        passwordPolicyService.validateOrThrow(req.newPassword());

        Instant now = Instant.now(clock);
        byte[] tokenHash = sha256(rawToken);

        PasswordResetToken token = passwordResetTokenRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> {
                    passwordResetAttemptService.record(
                            "",
                            null,
                            null,
                            null,
                            PasswordResetAttemptService.OUTCOME_RESET_INVALID_TOKEN
                    );
                    auditResetPassword(null, null, PasswordResetAttemptService.OUTCOME_RESET_INVALID_TOKEN);
                    return ApiException.badRequest("RESET_TOKEN_INVALID", "Reset token is invalid");
                });

        if (token.isUsed()) {
            passwordResetAttemptService.record(
                    "",
                    token.getUserId(),
                    token.getRequestedIp(),
                    token.getUserAgent(),
                    PasswordResetAttemptService.OUTCOME_RESET_ALREADY_USED
            );
            auditResetPassword(token.getUserId(), token.getRequestedIp(), PasswordResetAttemptService.OUTCOME_RESET_ALREADY_USED);
            throw ApiException.badRequest("RESET_TOKEN_ALREADY_USED", "Reset token has already been used");
        }

        if (token.isRevoked()) {
            passwordResetAttemptService.record(
                    "",
                    token.getUserId(),
                    token.getRequestedIp(),
                    token.getUserAgent(),
                    PasswordResetAttemptService.OUTCOME_RESET_REVOKED
            );
            auditResetPassword(token.getUserId(), token.getRequestedIp(), PasswordResetAttemptService.OUTCOME_RESET_REVOKED);
            throw ApiException.badRequest("RESET_TOKEN_REVOKED", "Reset token has been revoked");
        }

        if (token.isExpiredAt(now)) {
            passwordResetAttemptService.record(
                    "",
                    token.getUserId(),
                    token.getRequestedIp(),
                    token.getUserAgent(),
                    PasswordResetAttemptService.OUTCOME_RESET_EXPIRED
            );
            auditResetPassword(token.getUserId(), token.getRequestedIp(), PasswordResetAttemptService.OUTCOME_RESET_EXPIRED);
            throw ApiException.badRequest("RESET_TOKEN_EXPIRED", "Reset token has expired");
        }

        User user = userRepository.findByIdAndDeletedAtIsNull(token.getUserId())
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            passwordResetAttemptService.record(
                    user.getEmail(),
                    user.getId(),
                    token.getRequestedIp(),
                    token.getUserAgent(),
                    PasswordResetAttemptService.OUTCOME_RESET_USER_NOT_ACTIVE
            );
            auditResetPassword(user.getId(), token.getRequestedIp(), PasswordResetAttemptService.OUTCOME_RESET_USER_NOT_ACTIVE);
            throw ApiException.badRequest("USER_DISABLED", "User is not active");
        }

        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        user.setRequirePasswordChange(false);
        userRepository.save(user);

        token.markUsed(now);
        passwordResetTokenRepository.save(token);

        passwordResetTokenRepository.revokeAllActiveByUserId(
                user.getId(),
                now,
                RESET_TOKEN_REVOKE_REASON_COMPLETED
        );

        userSessionRepository.revokeAllActiveByUserId(
                user.getId(),
                now,
                REVOKE_REASON_PASSWORD_RESET
        );

        passwordResetAttemptService.record(
                user.getEmail(),
                user.getId(),
                token.getRequestedIp(),
                token.getUserAgent(),
                PasswordResetAttemptService.OUTCOME_RESET_SUCCESS
        );
        auditResetPassword(user.getId(), token.getRequestedIp(), PasswordResetAttemptService.OUTCOME_RESET_SUCCESS);
    }

    @Transactional
    public void logoutAll(UUID userId) {
        User user = getCurrentActiveUserOrThrow(userId);
        Instant now = Instant.now(clock);
        userSessionRepository.revokeAllActiveByUserId(user.getId(), now, REVOKE_REASON_LOGOUT_ALL);
    }

    @Transactional(readOnly = true)
    public User getCurrentUser(UUID userId) {
        return getCurrentActiveUserOrThrow(userId);
    }

    @Transactional
    public User updateMe(UUID userId, UpdateMeRequest req) {
        rejectUnknownFields(req.getUnknownFields());

        User user = getCurrentActiveUserOrThrow(userId);

        boolean changed = false;

        if (req.getFullName() != null) {
            String fullName = normalizeText(req.getFullName());
            if (!StringUtils.hasText(fullName) || fullName.length() < 2 || fullName.length() > 120) {
                throw ApiException.badRequest("FULL_NAME_INVALID", "fullName must be between 2 and 120 characters");
            }
            user.setFullName(fullName);
            changed = true;
        }

        if (req.getAvatarUrl() != null) {
            String avatarUrl = normalizeNullable(req.getAvatarUrl());
            if (avatarUrl != null && avatarUrl.length() > 500) {
                throw ApiException.badRequest("AVATAR_URL_TOO_LONG", "avatarUrl must be at most 500 characters");
            }
            user.setAvatarUrl(avatarUrl);
            changed = true;
        }

        if (!changed) {
            throw ApiException.badRequest("NO_FIELDS_TO_UPDATE", "At least one updatable field is required");
        }

        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest req) {
        rejectUnknownFields(req.getUnknownFields());

        User user = getCurrentActiveUserOrThrow(userId);

        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
            throw ApiException.unprocessable("CURRENT_PASSWORD_INCORRECT", "Current password is incorrect");
        }

        if (req.getCurrentPassword().equals(req.getNewPassword())) {
            throw ApiException.unprocessable(
                    "NEW_PASSWORD_MUST_BE_DIFFERENT",
                    "New password must be different from current password"
            );
        }

        passwordPolicyService.validateOrThrow(req.getNewPassword());

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        user.setRequirePasswordChange(false);
        userRepository.save(user);

        Instant now = Instant.now(clock);
        userSessionRepository.revokeAllActiveByUserId(user.getId(), now, REVOKE_REASON_PASSWORD_CHANGED);
    }

    private User getCurrentActiveUserOrThrow(UUID userId) {
        if (userId == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Missing JWT principal");
        }

        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw ApiException.unauthorized("USER_DISABLED", "User is not active");
        }

        return user;
    }

    private AuthDtos.LoginResponse toLoginResponse(User user, JwtService.TokenBundle tokenBundle) {
        return new AuthDtos.LoginResponse(
                TOKEN_TYPE_BEARER,
                tokenBundle.accessToken(),
                tokenBundle.accessTokenExpiresAt(),
                tokenBundle.refreshToken(),
                tokenBundle.refreshTokenExpiresAt(),
                tokenBundle.sessionId(),
                new AuthDtos.UserInfo(
                        user.getId(),
                        user.getEmail(),
                        user.getFullName(),
                        resolvePlatformRole(user).name()
                )
        );
    }

    private PlatformRole resolvePlatformRole(User user) {
        return user.getPlatformRole() != null ? user.getPlatformRole() : PlatformRole.USER;
    }

    private void rejectUnknownFields(Set<String> unknownFields) {
        if (unknownFields == null || unknownFields.isEmpty()) {
            return;
        }

        throw ApiException.badRequest(
                "INVALID_REQUEST_BODY",
                "Unknown field(s): " + String.join(", ", unknownFields)
        );
    }

    private ApiException mapRegisterConflict(DataIntegrityViolationException ex) {
        String msg = extractErrorMessage(ex);

        if (msg.contains("uk_users_email_norm")) {
            return ApiException.conflict("EMAIL_ALREADY_EXISTS", "Email already exists");
        }

        if (msg.contains("uk_users_user_code")) {
            return ApiException.conflict("USER_CODE_ALREADY_EXISTS", "User code already exists");
        }

        if (msg.contains("email_norm")) {
            return ApiException.conflict("EMAIL_ALREADY_EXISTS", "Email already exists");
        }

        if (msg.contains("user_code")) {
            return ApiException.conflict("USER_CODE_ALREADY_EXISTS", "User code already exists");
        }

        return ApiException.conflict("USER_ALREADY_EXISTS", "User already exists");
    }

    private String extractErrorMessage(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String msg = current.getMessage();
        return msg == null ? "" : msg.toLowerCase(Locale.ROOT);
    }

    private String buildResetUrl(String frontendResetUrl, String token) {
        if (!StringUtils.hasText(frontendResetUrl)) {
            throw ApiException.badRequest(
                    "PASSWORD_RESET_FRONTEND_URL_MISSING",
                    "password reset frontend URL is not configured"
            );
        }

        String separator = frontendResetUrl.contains("?") ? "&" : "?";
        return frontendResetUrl + separator + "token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private byte[] sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(raw.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to hash value", ex);
        }
    }

    private void auditForgotPassword(String emailNorm, UUID userId, String ipAddress, String outcome) {
        securityAuditLog.info(
                "event=forgot_password userId={} email={} ip={} outcome={}",
                userId,
                maskEmail(emailNorm),
                ipAddress,
                outcome
        );
    }

    private void auditResetPassword(UUID userId, String ipAddress, String outcome) {
        securityAuditLog.info(
                "event=reset_password userId={} ip={} outcome={}",
                userId,
                ipAddress,
                outcome
        );
    }

    private String maskEmail(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return "***";
        }

        String[] parts = email.split("@", 2);
        String local = parts[0];
        String domain = parts[1];

        if (local.length() <= 2) {
            return "***@" + domain;
        }

        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + "@" + domain;
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String truncateUserAgent(String userAgent) {
        String normalized = normalizeNullable(userAgent);
        if (normalized == null) {
            return null;
        }
        return normalized.length() <= 255 ? normalized : normalized.substring(0, 255);
    }
}