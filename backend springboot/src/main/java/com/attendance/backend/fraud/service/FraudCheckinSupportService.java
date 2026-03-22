package com.attendance.backend.fraud.service;

import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.fraud.domain.CheckinFailureCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class FraudCheckinSupportService {

    private static final Logger log = LoggerFactory.getLogger(FraudCheckinSupportService.class);

    private final FraudSessionLookupService fraudSessionLookupService;
    private final FraudDetectionService fraudDetectionService;
    private final FraudCheckinFailureCodeMapper failureCodeMapper;
    private final ObjectMapper objectMapper;

    public FraudCheckinSupportService(
            FraudSessionLookupService fraudSessionLookupService,
            FraudDetectionService fraudDetectionService,
            FraudCheckinFailureCodeMapper failureCodeMapper,
            ObjectMapper objectMapper
    ) {
        this.fraudSessionLookupService = fraudSessionLookupService;
        this.fraudDetectionService = fraudDetectionService;
        this.failureCodeMapper = failureCodeMapper;
        this.objectMapper = objectMapper;
    }

    public void captureSuccess(
            UUID sessionId,
            UUID actorUserId,
            String rawToken,
            String resolvedQrTokenId,
            String deviceId,
            BigDecimal geoLat,
            BigDecimal geoLng,
            Integer distanceMeter,
            String ipAddress,
            String userAgent
    ) {
        try {
            UUID groupId = fraudSessionLookupService.requireGroupIdBySessionId(sessionId);

            fraudDetectionService.handleSuccessfulAttempt(
                    new FraudDetectionService.CheckinAttemptContext(
                            groupId,
                            sessionId,
                            actorUserId,
                            resolvedQrTokenId,
                            sha256Bytes(rawToken),
                            trimToNull(deviceId),
                            trimToNull(ipAddress),
                            trimToNull(userAgent),
                            geoLat,
                            geoLng,
                            distanceMeter,
                            null,
                            buildPayloadJson(
                                    sessionId,
                                    actorUserId,
                                    rawToken,
                                    resolvedQrTokenId,
                                    deviceId,
                                    geoLat,
                                    geoLng,
                                    distanceMeter,
                                    ipAddress,
                                    userAgent,
                                    "SUCCESS",
                                    null
                            )
                    )
            );
        } catch (Exception ex) {
            log.warn("Fraud capture success path failed for sessionId={}, userId={}", sessionId, actorUserId, ex);
        }
    }

    public void captureFailure(
            UUID sessionId,
            UUID actorUserId,
            String rawToken,
            String resolvedQrTokenIdOrNull,
            String deviceId,
            BigDecimal geoLat,
            BigDecimal geoLng,
            Integer distanceMeterOrNull,
            String ipAddress,
            String userAgent,
            ApiException ex
    ) {
        try {
            UUID groupId = fraudSessionLookupService.requireGroupIdBySessionId(sessionId);
            CheckinFailureCode failureCode = failureCodeMapper.map(ex);

            fraudDetectionService.handleFailedAttempt(
                    new FraudDetectionService.CheckinAttemptContext(
                            groupId,
                            sessionId,
                            actorUserId,
                            resolvedQrTokenIdOrNull,
                            sha256Bytes(rawToken),
                            trimToNull(deviceId),
                            trimToNull(ipAddress),
                            trimToNull(userAgent),
                            geoLat,
                            geoLng,
                            distanceMeterOrNull,
                            failureCode.name(),
                            buildPayloadJson(
                                    sessionId,
                                    actorUserId,
                                    rawToken,
                                    resolvedQrTokenIdOrNull,
                                    deviceId,
                                    geoLat,
                                    geoLng,
                                    distanceMeterOrNull,
                                    ipAddress,
                                    userAgent,
                                    "FAIL",
                                    failureCode.name()
                            )
                    )
            );
        } catch (Exception fraudEx) {
            log.warn("Fraud capture failure path failed for sessionId={}, userId={}", sessionId, actorUserId, fraudEx);
        }
    }

    private String buildPayloadJson(
            UUID sessionId,
            UUID actorUserId,
            String rawToken,
            String resolvedQrTokenId,
            String deviceId,
            BigDecimal geoLat,
            BigDecimal geoLng,
            Integer distanceMeter,
            String ipAddress,
            String userAgent,
            String outcome,
            String failureCode
    ) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("capturedAt", Instant.now().toString());
            payload.put("sessionId", sessionId);
            payload.put("actorUserId", actorUserId);
            payload.put("resolvedQrTokenId", resolvedQrTokenId);
            payload.put("tokenHashHex", sha256Hex(rawToken));
            payload.put("deviceId", trimToNull(deviceId));
            payload.put("geoLat", geoLat);
            payload.put("geoLng", geoLng);
            payload.put("distanceMeter", distanceMeter);
            payload.put("ipAddress", trimToNull(ipAddress));
            payload.put("userAgent", trimToNull(userAgent));
            payload.put("outcome", outcome);
            payload.put("failureCode", failureCode);
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            return "{\"payloadSerialization\":\"FAILED\"}";
        }
    }

    private byte[] sha256Bytes(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash QR token", ex);
        }
    }

    private String sha256Hex(String value) {
        byte[] bytes = sha256Bytes(value);
        if (bytes == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}