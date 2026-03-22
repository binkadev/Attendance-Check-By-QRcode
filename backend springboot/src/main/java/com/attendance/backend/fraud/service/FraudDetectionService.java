package com.attendance.backend.fraud.service;

import java.math.BigDecimal;
import java.util.UUID;

public interface FraudDetectionService {

    void handleSuccessfulAttempt(CheckinAttemptContext context);

    void handleFailedAttempt(CheckinAttemptContext context);

    record CheckinAttemptContext(
        UUID groupId,
        UUID sessionId,
        UUID userId,
        String qrTokenId,
        byte[] tokenHash,
        String deviceId,
        String ipAddress,
        String userAgent,
        BigDecimal geoLat,
        BigDecimal geoLng,
        Integer distanceMeter,
        String failureCode,
        String payloadJson
    ) {
    }
}
