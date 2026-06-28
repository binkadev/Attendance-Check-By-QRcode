package com.attendance.backend.fraud.service;

import com.attendance.backend.fraud.domain.CheckinAttemptOutcome;
import com.attendance.backend.fraud.domain.CheckinFailureCode;
import com.attendance.backend.fraud.domain.FraudIncidentSeverity;
import com.attendance.backend.fraud.domain.FraudIncidentType;
import com.attendance.backend.fraud.entity.CheckinAttemptLog;
import com.attendance.backend.fraud.repository.CheckinAttemptLogRepository;
import com.attendance.backend.fraud.repository.CheckinAttemptLogRepositoryCustom.DeviceMultiAccountAggregate;
import com.attendance.backend.fraud.repository.CheckinAttemptLogRepositoryCustom.IpBurstAggregate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class FraudDetectionServiceImpl implements FraudDetectionService {

    private static final int FAILED_TOKEN_THRESHOLD = 5;
    private static final int FAILED_TOKEN_WINDOW_SECONDS = 10 * 60;

    private static final int WRONG_OR_EXPIRED_THRESHOLD = 3;
    private static final int WRONG_OR_EXPIRED_WINDOW_SECONDS = 5 * 60;

    private static final int OUT_OF_RANGE_THRESHOLD = 3;
    private static final int OUT_OF_RANGE_WINDOW_SECONDS = 15 * 60;

    private static final int IP_TOTAL_THRESHOLD = 5;
    private static final int IP_FAIL_THRESHOLD = 3;
    private static final int IP_WINDOW_SECONDS = 5 * 60;

    private static final int SHARED_DEVICE_THRESHOLD = 2;
    private static final int SHARED_DEVICE_WINDOW_SECONDS = 24 * 60 * 60;

    private final CheckinAttemptLogRepository checkinAttemptLogRepository;
    private final FraudIncidentService fraudIncidentService;
    private final ObjectMapper objectMapper;

    public FraudDetectionServiceImpl(
            CheckinAttemptLogRepository checkinAttemptLogRepository,
            FraudIncidentService fraudIncidentService,
            ObjectMapper objectMapper
    ) {
        this.checkinAttemptLogRepository = checkinAttemptLogRepository;
        this.fraudIncidentService = fraudIncidentService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleSuccessfulAttempt(CheckinAttemptContext context) {
        persistAttempt(context, CheckinAttemptOutcome.SUCCESS, null);
        detectSharedDevice(context);
        detectIpBurst(context);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFailedAttempt(CheckinAttemptContext context) {
        CheckinFailureCode failureCode = resolveFailureCode(context.failureCode());
        persistAttempt(context, CheckinAttemptOutcome.FAIL, failureCode);
        detectRepeatedFailedToken(context, failureCode);
        detectWrongOrExpired(context, failureCode);
        detectOutOfRange(context, failureCode);
        detectSharedDevice(context);
        detectIpBurst(context);
    }

    protected void persistAttempt(
            CheckinAttemptContext context,
            CheckinAttemptOutcome outcome,
            CheckinFailureCode failureCode
    ) {
        CheckinAttemptLog log = new CheckinAttemptLog();
        log.setId(UUID.randomUUID());
        log.setGroupId(context.groupId());
        log.setSessionId(context.sessionId());
        log.setUserId(context.userId());
        log.setQrTokenId(context.qrTokenId());
        log.setTokenHash(context.tokenHash());
        log.setDeviceId(context.deviceId());
        log.setIpAddress(context.ipAddress());
        log.setUserAgent(context.userAgent());
        log.setGeoLat(context.geoLat());
        log.setGeoLng(context.geoLng());
        log.setDistanceMeter(context.distanceMeter());
        log.setOutcome(outcome);
        log.setFailureCode(failureCode);
        log.setPayloadJson(context.payloadJson());
        log.setCreatedAt(Instant.now());

        checkinAttemptLogRepository.saveAndFlush(log);
    }

    private void detectRepeatedFailedToken(CheckinAttemptContext context, CheckinFailureCode failureCode) {
        if (context.tokenHash() == null) {
            return;
        }
        if (!Set.of(
                CheckinFailureCode.TOKEN_INVALID,
                CheckinFailureCode.TOKEN_MALFORMED,
                CheckinFailureCode.TOKEN_NOT_FOUND
        ).contains(failureCode)) {
            return;
        }

        Instant from = Instant.now().minusSeconds(FAILED_TOKEN_WINDOW_SECONDS);

        long count = checkinAttemptLogRepository.countBySessionUserTokenHashFailureCodesWithinWindow(
                context.sessionId(),
                context.userId(),
                context.tokenHash(),
                Set.of(
                        CheckinFailureCode.TOKEN_INVALID,
                        CheckinFailureCode.TOKEN_MALFORMED,
                        CheckinFailureCode.TOKEN_NOT_FOUND
                ),
                from
        );

        if (count < FAILED_TOKEN_THRESHOLD) {
            return;
        }

        String dedupKey = sha256Hex(
                "REPEATED_FAILED_QR_TOKEN:"
                        + context.sessionId()
                        + ":"
                        + context.userId()
                        + ":"
                        + HexFormat.of().formatHex(context.tokenHash())
        );

        openIncident(
                context,
                FraudIncidentType.REPEATED_FAILED_QR_TOKEN,
                FraudIncidentSeverity.MEDIUM,
                dedupKey,
                Map.of(
                        "threshold", FAILED_TOKEN_THRESHOLD,
                        "ruleWindowSeconds", FAILED_TOKEN_WINDOW_SECONDS,
                        "lastFailureCode", failureCode.name(),
                        "sampleAttemptIds", checkinAttemptLogRepository.findRecentAttemptIdsForEvidence(
                                context.sessionId(),
                                context.userId(),
                                from,
                                5
                        )
                )
        );
    }

    private void detectWrongOrExpired(CheckinAttemptContext context, CheckinFailureCode failureCode) {
        if (failureCode != CheckinFailureCode.TOKEN_WRONG_SESSION
                && failureCode != CheckinFailureCode.TOKEN_EXPIRED) {
            return;
        }

        Instant from = Instant.now().minusSeconds(WRONG_OR_EXPIRED_WINDOW_SECONDS);

        long count = checkinAttemptLogRepository.countBySessionUserFailureCodeWithinWindow(
                context.sessionId(),
                context.userId(),
                failureCode,
                from
        );

        if (count < WRONG_OR_EXPIRED_THRESHOLD) {
            return;
        }

        FraudIncidentType type = failureCode == CheckinFailureCode.TOKEN_WRONG_SESSION
                ? FraudIncidentType.WRONG_SESSION_QR_TOKEN
                : FraudIncidentType.EXPIRED_QR_TOKEN;

        FraudIncidentSeverity severity = failureCode == CheckinFailureCode.TOKEN_WRONG_SESSION
                ? FraudIncidentSeverity.HIGH
                : FraudIncidentSeverity.MEDIUM;

        String dedupKey = sha256Hex(
                type.name()
                        + ":"
                        + context.sessionId()
                        + ":"
                        + context.userId()
                        + ":"
                        + safeHex(context.tokenHash())
        );

        openIncident(
                context,
                type,
                severity,
                dedupKey,
                Map.of(
                        "threshold", WRONG_OR_EXPIRED_THRESHOLD,
                        "ruleWindowSeconds", WRONG_OR_EXPIRED_WINDOW_SECONDS,
                        "lastFailureCode", failureCode.name(),
                        "sampleAttemptIds", checkinAttemptLogRepository.findRecentAttemptIdsForEvidence(
                                context.sessionId(),
                                context.userId(),
                                from,
                                5
                        )
                )
        );
    }

    private void detectOutOfRange(CheckinAttemptContext context, CheckinFailureCode failureCode) {
        if (failureCode != CheckinFailureCode.OUT_OF_RANGE) {
            return;
        }

        Instant from = Instant.now().minusSeconds(OUT_OF_RANGE_WINDOW_SECONDS);

        long count = checkinAttemptLogRepository.countBySessionUserFailureCodeWithinWindow(
                context.sessionId(),
                context.userId(),
                CheckinFailureCode.OUT_OF_RANGE,
                from
        );

        if (count < OUT_OF_RANGE_THRESHOLD) {
            return;
        }

        String dedupKey = sha256Hex("REPEATED_OUT_OF_RANGE:" + context.sessionId() + ":" + context.userId());

        openIncident(
                context,
                FraudIncidentType.REPEATED_OUT_OF_RANGE,
                FraudIncidentSeverity.HIGH,
                dedupKey,
                Map.of(
                        "threshold", OUT_OF_RANGE_THRESHOLD,
                        "ruleWindowSeconds", OUT_OF_RANGE_WINDOW_SECONDS,
                        "maxDistanceMeter", context.distanceMeter(),
                        "lastFailureCode", failureCode.name(),
                        "sampleAttemptIds", checkinAttemptLogRepository.findRecentAttemptIdsForEvidence(
                                context.sessionId(),
                                context.userId(),
                                from,
                                5
                        )
                )
        );
    }

    private void detectIpBurst(CheckinAttemptContext context) {
        if (context.ipAddress() == null || context.ipAddress().isBlank()) {
            return;
        }

        Instant from = Instant.now().minusSeconds(IP_WINDOW_SECONDS);

        IpBurstAggregate aggregate = checkinAttemptLogRepository.aggregateIpBurst(
                context.groupId(),
                context.ipAddress(),
                from
        );

        if (aggregate.totalAttempts() < IP_TOTAL_THRESHOLD && aggregate.failAttempts() < IP_FAIL_THRESHOLD) {
            return;
        }

        FraudIncidentSeverity severity = aggregate.failAttempts() >= IP_FAIL_THRESHOLD
                ? FraudIncidentSeverity.HIGH
                : FraudIncidentSeverity.MEDIUM;

        long bucket = Instant.now().getEpochSecond() / IP_WINDOW_SECONDS;

        String dedupKey = sha256Hex(
                "IP_BURST_MULTI_ATTEMPT:"
                        + context.groupId()
                        + ":"
                        + context.ipAddress()
                        + ":"
                        + bucket
        );

        openIncident(
                context,
                FraudIncidentType.IP_BURST_MULTI_ATTEMPT,
                severity,
                dedupKey,
                Map.of(
                        "threshold", IP_TOTAL_THRESHOLD,
                        "ruleWindowSeconds", IP_WINDOW_SECONDS,
                        "distinctIpCount", 1,
                        "notes", List.of("Group-scoped IP burst rule to reduce campus Wi-Fi false positives"),
                        "sampleIpAddresses", List.of(context.ipAddress())
                )
        );
    }

    private void detectSharedDevice(CheckinAttemptContext context) {
        if (context.deviceId() == null || context.deviceId().isBlank()) {
            return;
        }

        Instant from = Instant.now().minusSeconds(SHARED_DEVICE_WINDOW_SECONDS);

        DeviceMultiAccountAggregate aggregate = checkinAttemptLogRepository.aggregateSharedDevice(
                context.groupId(),
                context.deviceId(),
                from
        );

        if (aggregate.distinctUserCount() < SHARED_DEVICE_THRESHOLD) {
            return;
        }

        Instant dayBucket = Instant.now().truncatedTo(ChronoUnit.DAYS);

        String dedupKey = sha256Hex(
                "SHARED_DEVICE_MULTI_ACCOUNT:"
                        + context.groupId()
                        + ":"
                        + context.deviceId()
                        + ":"
                        + dayBucket
        );

        openIncident(
                context,
                FraudIncidentType.SHARED_DEVICE_MULTI_ACCOUNT,
                FraudIncidentSeverity.HIGH,
                dedupKey,
                Map.of(
                        "threshold", SHARED_DEVICE_THRESHOLD,
                        "ruleWindowSeconds", SHARED_DEVICE_WINDOW_SECONDS,
                        "distinctUserCount", aggregate.distinctUserCount(),
                        "sampleDeviceIds", List.of(context.deviceId()),
                        "notes", List.of("Threshold starts at 2 because same physical device across multiple accounts must be reviewed")
                )
        );
    }

    private void openIncident(
            CheckinAttemptContext context,
            FraudIncidentType type,
            FraudIncidentSeverity severity,
            String dedupKey,
            Map<String, Object> evidence
    ) {
        try {
            fraudIncidentService.openOrBump(
                    new FraudIncidentService.OpenFraudIncidentCommand(
                            context.groupId(),
                            context.sessionId(),
                            context.userId(),
                            type,
                            severity,
                            dedupKey,
                            Instant.now(),
                            objectMapper.writeValueAsString(evidence)
                    )
            );
        } catch (Exception ignored) {
            // Swallow by design: fraud side-effect must not break business check-in.
        }
    }

    private CheckinFailureCode resolveFailureCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return CheckinFailureCode.UNKNOWN;
        }
        try {
            return CheckinFailureCode.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return CheckinFailureCode.UNKNOWN;
        }
    }

    private String safeHex(byte[] tokenHash) {
        return tokenHash == null ? "NO_TOKEN_HASH" : HexFormat.of().formatHex(tokenHash);
    }

    private String sha256Hex(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash fraud dedup key", ex);
        }
    }
}
