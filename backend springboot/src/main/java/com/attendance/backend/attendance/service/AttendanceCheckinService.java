package com.attendance.backend.attendance.service;

import com.attendance.backend.attendance.repository.AttendanceSessionRepository;
import com.attendance.backend.attendance.repository.SessionAttendanceRepository;
import com.attendance.backend.common.db.DbErrorTranslator;
import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.domain.entity.AttendanceEvent;
import com.attendance.backend.domain.entity.AttendanceSession;
import com.attendance.backend.domain.entity.QrToken;
import com.attendance.backend.domain.entity.SessionAttendance;
import com.attendance.backend.domain.enums.AttendanceStatus;
import com.attendance.backend.domain.enums.CheckInMethod;
import com.attendance.backend.domain.enums.EventType;
import com.attendance.backend.domain.enums.SessionStatus;
import com.attendance.backend.fraud.domain.CheckinFailureCode;
import com.attendance.backend.fraud.service.FraudDetectionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class AttendanceCheckinService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceCheckinService.class);

    private final AttendanceSessionRepository attendanceSessionRepository;
    private final SessionAttendanceRepository sessionAttendanceRepository;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;
    private final Clock clock = Clock.systemUTC();
    private final DbErrorTranslator dbErrorTranslator;
    private final FraudDetectionService fraudDetectionService;

    public AttendanceCheckinService(
            AttendanceSessionRepository attendanceSessionRepository,
            SessionAttendanceRepository sessionAttendanceRepository,
            EntityManager entityManager,
            ObjectMapper objectMapper,
            DbErrorTranslator dbErrorTranslator,
            FraudDetectionService fraudDetectionService
    ) {
        this.attendanceSessionRepository = attendanceSessionRepository;
        this.sessionAttendanceRepository = sessionAttendanceRepository;
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
        this.dbErrorTranslator = dbErrorTranslator;
        this.fraudDetectionService = fraudDetectionService;
    }

    /**
     * RULE CHỐT:
     * - 1 session + 1 user chỉ được ghi nhận check-in thành công 1 lần.
     * - Request quét lặp do camera/app fire nhiều lần sẽ trả lại kết quả cũ 200 OK.
     * - Không update lại checkInAt cho request duplicate.
     * - Không tạo AttendanceEvent mới cho request duplicate.
     * - Reject if now < checkin_open_at.
     * - Reject if now > checkin_close_at.
     * - lateThreshold = checkin_open_at + late_after_minutes.
     *   now <= threshold => PRESENT.
     *   threshold < now <= close => LATE.
     */
    @Transactional
    public QrCheckinResult qrCheckin(QrCheckinCommand cmd) {
        final Instant now = Instant.now(clock);

        AttendanceSession session = attendanceSessionRepository.findByIdForShare(cmd.sessionId())
                .orElseThrow(() -> ApiException.notFound("SESSION_NOT_FOUND", "Session not found"));

        String resolvedQrTokenId = null;
        Instant openAt = null;
        Instant closeAt = null;
        Instant lateThreshold = null;
        AttendanceStatus computed = null;

        try {
            if (session.getStatus() != SessionStatus.OPEN) {
                throw ApiException.conflict("SESSION_NOT_OPEN", "Session is not OPEN");
            }

            ensureApprovedMember(session.getGroupId(), cmd.userId());

            QrToken token = verifyQrToken(cmd.token(), cmd.sessionId(), now);
            resolvedQrTokenId = token.getTokenId();

            openAt = session.getCheckinOpenAt() != null
                    ? session.getCheckinOpenAt()
                    : session.getStartAt();

            closeAt = session.getCheckinCloseAt() != null
                    ? session.getCheckinCloseAt()
                    : openAt.plus(Duration.ofMinutes(session.getTimeWindowMinutes()));

            if (closeAt.isBefore(openAt)) {
                throw ApiException.unprocessable(
                        "SESSION_TIME_INVALID",
                        "checkin_close_at is before checkin_open_at"
                );
            }

            if (now.isBefore(openAt)) {
                throw ApiException.conflict("CHECKIN_NOT_OPEN_YET", "Check-in not open yet");
            }

            if (now.isAfter(closeAt)) {
                throw ApiException.conflict("CHECKIN_CLOSED", "Check-in window already closed");
            }

            lateThreshold = openAt.plus(Duration.ofMinutes(session.getLateAfterMinutes()));
            computed = now.isAfter(lateThreshold) ? AttendanceStatus.LATE : AttendanceStatus.PRESENT;

            /*
             * Quan trọng:
             * Scanner mobile có thể gọi API 2 lần rất nhanh.
             *
             * check_in_method trong entity hiện là nullable=false, nên row placeholder
             * không được insert NULL. Trạng thái "chưa điểm danh" sẽ là:
             *
             * ABSENT + check_in_at NULL + qr_token_id NULL + check_in_method QR
             *
             * Khi reset cũng nên về trạng thái như vậy.
             */
            SessionAttendance attendance = getOrCreateAttendanceLocked(cmd.sessionId(), cmd.userId(), now);

            if (attendance.attendanceStatus == AttendanceStatus.EXCUSED || attendance.excusedByRequestId != null) {
                throw ApiException.conflict(
                        "ATTENDANCE_ALREADY_EXCUSED",
                        "Attendance already excused by approved absence request"
                );
            }

            /*
             * Idempotent duplicate:
             * Nếu request trước đã ghi nhận PRESENT/LATE hoặc đã có checkInAt/qrTokenId,
             * request sau trả lại kết quả cũ 200 OK.
             */
            if (isAlreadyCheckedIn(attendance)) {
                return toExistingQrCheckinResult(cmd.sessionId(), cmd.userId(), attendance);
            }

            /*
             * Nếu giảng viên đã chủ động mark ABSENT thủ công thì không cho sinh viên QR đè lên.
             * Muốn cho QR lại thì giảng viên phải gọi reset để đưa về:
             * ABSENT + checkInAt null + qrTokenId null + checkInMethod QR.
             */
            if (isLockedByManualAbsent(attendance)) {
                throw ApiException.conflict(
                        "ALREADY_CHECKED_IN",
                        "User attendance was manually marked ABSENT for this session"
                );
            }

            AttendanceStatus oldStatus = attendance.attendanceStatus;

            attendance.attendanceStatus = computed;
            attendance.checkInAt = now;
            attendance.checkInMethod = CheckInMethod.QR;
            attendance.qrTokenId = resolvedQrTokenId;
            attendance.deviceId = blankToNull(cmd.deviceId());
            attendance.ipAddress = blankToNull(cmd.ipAddress());
            attendance.userAgent = blankToNull(cmd.userAgent());
            attendance.geoLat = cmd.geoLat();
            attendance.geoLng = cmd.geoLng();
            attendance.distanceMeter = cmd.distanceMeter();
            attendance.suspiciousFlag = false;
            attendance.suspiciousReason = null;
            attendance.updatedAt = now;

            sessionAttendanceRepository.saveAndFlush(attendance);

            AttendanceEvent event = new AttendanceEvent();
            event.id = UUID.randomUUID();
            event.sessionId = cmd.sessionId();
            event.userId = cmd.userId();
            event.actorUserId = cmd.userId();
            event.eventType = EventType.CHECKIN_QR;
            event.oldStatus = oldStatus;
            event.newStatus = computed;
            event.qrTokenId = resolvedQrTokenId;

            ObjectNode payload = objectMapper.createObjectNode();
            putIfNotNull(payload, "deviceId", cmd.deviceId());
            putIfNotNull(payload, "ipAddress", cmd.ipAddress());
            putIfNotNull(payload, "userAgent", cmd.userAgent());

            if (cmd.geoLat() != null) {
                payload.put("geoLat", cmd.geoLat());
            }
            if (cmd.geoLng() != null) {
                payload.put("geoLng", cmd.geoLng());
            }
            if (cmd.distanceMeter() != null) {
                payload.put("distanceMeter", cmd.distanceMeter());
            }

            payload.put("computedStatus", computed.name());
            payload.put("openAt", openAt.toString());
            payload.put("closeAt", closeAt.toString());
            payload.put("lateThreshold", lateThreshold.toString());
            payload.put("oneTimeCheckin", true);

            event.eventPayload = payload;
            event.createdAt = now;

            entityManager.persist(event);
            entityManager.flush();

            registerFraudSuccessAfterCommit(
                    session.getGroupId(),
                    cmd,
                    resolvedQrTokenId,
                    computed,
                    now,
                    openAt,
                    closeAt,
                    lateThreshold
            );

            return new QrCheckinResult(
                    cmd.sessionId(),
                    cmd.userId(),
                    computed,
                    now,
                    resolvedQrTokenId
            );
        } catch (ApiException ex) {
            captureFraudFailure(
                    session.getGroupId(),
                    cmd,
                    resolvedQrTokenId,
                    now,
                    openAt,
                    closeAt,
                    lateThreshold,
                    ex
            );
            throw ex;
        } catch (DataIntegrityViolationException ex) {
            throw dbErrorTranslator.translate(ex);
        }
    }

    private void registerFraudSuccessAfterCommit(
            UUID groupId,
            QrCheckinCommand cmd,
            String resolvedQrTokenId,
            AttendanceStatus computedStatus,
            Instant detectedAt,
            Instant openAt,
            Instant closeAt,
            Instant lateThreshold
    ) {
        FraudDetectionService.CheckinAttemptContext context = buildFraudContext(
                groupId,
                cmd,
                resolvedQrTokenId,
                null,
                detectedAt,
                computedStatus,
                openAt,
                closeAt,
                lateThreshold
        );

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        fraudDetectionService.handleSuccessfulAttempt(context);
                    } catch (Exception ex) {
                        log.warn(
                                "Fraud success capture failed after commit for sessionId={}, userId={}",
                                cmd.sessionId(),
                                cmd.userId(),
                                ex
                        );
                    }
                }
            });
            return;
        }

        try {
            fraudDetectionService.handleSuccessfulAttempt(context);
        } catch (Exception ex) {
            log.warn(
                    "Fraud success capture failed without transaction synchronization for sessionId={}, userId={}",
                    cmd.sessionId(),
                    cmd.userId(),
                    ex
            );
        }
    }

    private void captureFraudFailure(
            UUID groupId,
            QrCheckinCommand cmd,
            String resolvedQrTokenId,
            Instant detectedAt,
            Instant openAt,
            Instant closeAt,
            Instant lateThreshold,
            ApiException ex
    ) {
        try {
            FraudDetectionService.CheckinAttemptContext context = buildFraudContext(
                    groupId,
                    cmd,
                    resolvedQrTokenId,
                    mapFailureCode(ex),
                    detectedAt,
                    null,
                    openAt,
                    closeAt,
                    lateThreshold
            );
            fraudDetectionService.handleFailedAttempt(context);
        } catch (Exception fraudEx) {
            log.warn(
                    "Fraud failure capture failed for sessionId={}, userId={}, apiCode={}",
                    cmd.sessionId(),
                    cmd.userId(),
                    ex.getCode(),
                    fraudEx
            );
        }
    }

    private FraudDetectionService.CheckinAttemptContext buildFraudContext(
            UUID groupId,
            QrCheckinCommand cmd,
            String resolvedQrTokenId,
            CheckinFailureCode failureCode,
            Instant detectedAt,
            AttendanceStatus computedStatus,
            Instant openAt,
            Instant closeAt,
            Instant lateThreshold
    ) {
        return new FraudDetectionService.CheckinAttemptContext(
                groupId,
                cmd.sessionId(),
                cmd.userId(),
                resolvedQrTokenId,
                sha256Nullable(cmd.token()),
                blankToNull(cmd.deviceId()),
                blankToNull(cmd.ipAddress()),
                blankToNull(cmd.userAgent()),
                cmd.geoLat(),
                cmd.geoLng(),
                cmd.distanceMeter(),
                failureCode == null ? null : failureCode.name(),
                buildFraudPayloadJson(
                        cmd,
                        resolvedQrTokenId,
                        failureCode,
                        detectedAt,
                        computedStatus,
                        openAt,
                        closeAt,
                        lateThreshold
                )
        );
    }

    private String buildFraudPayloadJson(
            QrCheckinCommand cmd,
            String resolvedQrTokenId,
            CheckinFailureCode failureCode,
            Instant detectedAt,
            AttendanceStatus computedStatus,
            Instant openAt,
            Instant closeAt,
            Instant lateThreshold
    ) {
        try {
            ObjectNode payload = objectMapper.createObjectNode();

            payload.put("capturedAt", detectedAt.toString());
            payload.put("sessionId", cmd.sessionId().toString());
            payload.put("userId", cmd.userId().toString());

            if (resolvedQrTokenId != null) {
                payload.put("qrTokenId", resolvedQrTokenId);
            }

            String tokenHashHex = sha256HexNullable(cmd.token());
            if (tokenHashHex != null) {
                payload.put("tokenHashHex", tokenHashHex);
            }

            putIfNotNull(payload, "deviceId", cmd.deviceId());
            putIfNotNull(payload, "ipAddress", cmd.ipAddress());
            putIfNotNull(payload, "userAgent", cmd.userAgent());

            if (cmd.geoLat() != null) {
                payload.put("geoLat", cmd.geoLat());
            }
            if (cmd.geoLng() != null) {
                payload.put("geoLng", cmd.geoLng());
            }
            if (cmd.distanceMeter() != null) {
                payload.put("distanceMeter", cmd.distanceMeter());
            }

            if (failureCode == null) {
                payload.put("outcome", "SUCCESS");
            } else {
                payload.put("outcome", "FAIL");
                payload.put("failureCode", failureCode.name());
            }

            if (computedStatus != null) {
                payload.put("computedStatus", computedStatus.name());
            }
            if (openAt != null) {
                payload.put("openAt", openAt.toString());
            }
            if (closeAt != null) {
                payload.put("closeAt", closeAt.toString());
            }
            if (lateThreshold != null) {
                payload.put("lateThreshold", lateThreshold.toString());
            }

            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            return "{\"payloadSerialization\":\"FAILED\"}";
        }
    }

    private CheckinFailureCode mapFailureCode(ApiException ex) {
        if (ex == null || ex.getCode() == null || ex.getCode().isBlank()) {
            return CheckinFailureCode.UNKNOWN;
        }

        String code = ex.getCode().trim().toUpperCase();

        return switch (code) {
            case "QR_TOKEN_INVALID" -> CheckinFailureCode.TOKEN_INVALID;
            case "QR_TOKEN_INVALID_FORMAT", "QR_TOKEN_REQUIRED" -> CheckinFailureCode.TOKEN_MALFORMED;
            case "QR_TOKEN_EXPIRED" -> CheckinFailureCode.TOKEN_EXPIRED;
            case "QR_TOKEN_NOT_FOR_SESSION" -> CheckinFailureCode.TOKEN_WRONG_SESSION;
            case "QR_TOKEN_REVOKED" -> CheckinFailureCode.TOKEN_INVALID;

            case "CHECKIN_NOT_OPEN_YET" -> CheckinFailureCode.CHECKIN_NOT_OPEN_YET;
            case "CHECKIN_CLOSED" -> CheckinFailureCode.CHECKIN_CLOSED;

            case "ALREADY_CHECKED_IN" -> CheckinFailureCode.DUPLICATE_CHECKIN;

            case "NOT_A_GROUP_MEMBER" -> CheckinFailureCode.USER_NOT_MEMBER;

            case "SESSION_NOT_FOUND" -> CheckinFailureCode.SESSION_NOT_FOUND;
            case "SESSION_NOT_OPEN" -> CheckinFailureCode.SESSION_NOT_OPEN;

            default -> CheckinFailureCode.UNKNOWN;
        };
    }

    private SessionAttendance getOrCreateAttendanceLocked(UUID sessionId, UUID userId, Instant now) {
        entityManager.createNativeQuery("""
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
                    UUID_TO_BIN(:sessionId, 1),
                    UUID_TO_BIN(:userId, 1),
                    'ABSENT',
                    NULL,
                    'QR',
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
                    :now,
                    :now
                )
                ON DUPLICATE KEY UPDATE updated_at = updated_at
                """)
                .setParameter("sessionId", sessionId.toString())
                .setParameter("userId", userId.toString())
                .setParameter("now", Timestamp.from(now))
                .executeUpdate();

        return sessionAttendanceRepository
                .findBySessionAndUserForUpdate(sessionId, userId)
                .orElseThrow(() -> ApiException.notFound(
                        "ATTENDANCE_NOT_FOUND",
                        "Attendance row not found after create-or-lock"
                ));
    }

    private boolean isAlreadyCheckedIn(SessionAttendance attendance) {
        return attendance.checkInAt != null
                || attendance.qrTokenId != null
                || attendance.attendanceStatus == AttendanceStatus.PRESENT
                || attendance.attendanceStatus == AttendanceStatus.LATE;
    }

    private boolean isLockedByManualAbsent(SessionAttendance attendance) {
        return attendance.attendanceStatus == AttendanceStatus.ABSENT
                && attendance.checkInMethod == CheckInMethod.MANUAL;
    }

    private QrCheckinResult toExistingQrCheckinResult(
            UUID sessionId,
            UUID userId,
            SessionAttendance attendance
    ) {
        return new QrCheckinResult(
                sessionId,
                userId,
                attendance.attendanceStatus,
                attendance.checkInAt,
                attendance.qrTokenId
        );
    }

    private void ensureApprovedMember(UUID groupId, UUID userId) {
        Number cnt = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM group_members gm
                WHERE gm.group_id = UUID_TO_BIN(:groupId, 1)
                  AND gm.user_id  = UUID_TO_BIN(:userId, 1)
                  AND gm.member_status = 'APPROVED'
                """)
                .setParameter("groupId", groupId.toString())
                .setParameter("userId", userId.toString())
                .getSingleResult();

        if (cnt == null || cnt.longValue() == 0L) {
            throw ApiException.forbidden(
                    "NOT_A_GROUP_MEMBER",
                    "User is not an APPROVED member of this group"
            );
        }
    }

    private QrToken verifyQrToken(String rawToken, UUID expectedSessionId, Instant now) {
        ParsedToken parsed = parseToken(rawToken);

        QrToken token = entityManager.find(QrToken.class, parsed.tokenId(), LockModeType.PESSIMISTIC_READ);
        if (token == null) {
            throw ApiException.badRequest("QR_TOKEN_INVALID", "QR token is invalid");
        }

        if (!Objects.equals(token.getSessionId(), expectedSessionId)) {
            throw ApiException.conflict(
                    "QR_TOKEN_NOT_FOR_SESSION",
                    "QR token does not belong to this session"
            );
        }

        if (token.getRevokedAt() != null) {
            throw ApiException.conflict("QR_TOKEN_REVOKED", "QR token was revoked");
        }

        if (token.getExpiresAt() != null && now.isAfter(token.getExpiresAt())) {
            throw ApiException.conflict("QR_TOKEN_EXPIRED", "QR token expired");
        }

        byte[] stored = token.getTokenHash();
        byte[] hashSecret = sha256(parsed.secret());
        byte[] hashFull = sha256(parsed.normalizedPlainToken());

        boolean ok = MessageDigest.isEqual(stored, hashSecret)
                || MessageDigest.isEqual(stored, hashFull);

        if (!ok) {
            throw ApiException.badRequest("QR_TOKEN_INVALID", "QR token hash mismatch");
        }

        return token;
    }

    private ParsedToken parseToken(String rawToken) {
        String normalized = normalizeQrToken(rawToken);

        if (normalized == null || normalized.isBlank()) {
            throw ApiException.badRequest("QR_TOKEN_REQUIRED", "token is required");
        }

        String[] parts = normalized.split("\\.", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw ApiException.badRequest(
                    "QR_TOKEN_INVALID_FORMAT",
                    "Expected format: <tokenId>.<secret>"
            );
        }

        if (parts[0].length() > 64) {
            throw ApiException.badRequest(
                    "QR_TOKEN_INVALID_FORMAT",
                    "tokenId length > 64"
            );
        }

        return new ParsedToken(parts[0], parts[1], normalized);
    }

    private String normalizeQrToken(String rawToken) {
        if (rawToken == null) {
            return null;
        }

        String text = rawToken.trim();
        if (text.isEmpty()) {
            return null;
        }

        if (text.startsWith("{") && text.endsWith("}")) {
            try {
                JsonNode node = objectMapper.readTree(text);
                JsonNode tokenNode = node.get("token");
                if (tokenNode != null && !tokenNode.asText().isBlank()) {
                    return tokenNode.asText().trim();
                }
            } catch (Exception ignored) {
                // Fall through to normal parsing.
            }
        }

        if (text.contains("://") || text.contains("?token=") || text.contains("&token=")) {
            String tokenFromUrl = extractTokenFromUrl(text);
            if (tokenFromUrl != null && !tokenFromUrl.isBlank()) {
                return tokenFromUrl.trim();
            }
        }

        return text;
    }

    private String extractTokenFromUrl(String text) {
        try {
            URI uri = URI.create(text);
            String query = uri.getRawQuery();

            if (query == null || query.isBlank()) {
                return null;
            }

            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2 && "token".equals(kv[0])) {
                    return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                }
            }

            return null;
        } catch (Exception ignored) {
            int tokenIndex = text.indexOf("token=");
            if (tokenIndex < 0) {
                return null;
            }

            String value = text.substring(tokenIndex + "token=".length());
            int ampIndex = value.indexOf('&');

            if (ampIndex >= 0) {
                value = value.substring(0, ampIndex);
            }

            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        }
    }

    private byte[] sha256(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(raw.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw ApiException.badRequest("HASH_ERROR", "Cannot compute token hash");
        }
    }

    private byte[] sha256Nullable(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return sha256(raw);
    }

    private String sha256HexNullable(String raw) {
        byte[] hash = sha256Nullable(raw);
        if (hash == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }

        return sb.toString();
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void putIfNotNull(ObjectNode node, String key, String value) {
        if (value != null && !value.isBlank()) {
            node.put(key, value);
        }
    }

    public record QrCheckinCommand(
            UUID sessionId,
            UUID userId,
            String token,
            String deviceId,
            String ipAddress,
            String userAgent,
            BigDecimal geoLat,
            BigDecimal geoLng,
            Integer distanceMeter
    ) {
    }

    public record QrCheckinResult(
            UUID sessionId,
            UUID userId,
            AttendanceStatus attendanceStatus,
            Instant checkInAt,
            String qrTokenId
    ) {
    }

    private record ParsedToken(
            String tokenId,
            String secret,
            String normalizedPlainToken
    ) {
    }
}