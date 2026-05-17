package com.attendance.backend.fraud.dto;

import java.util.List;
import java.util.UUID;

public record FraudIncidentEvidenceSummaryResponse(
        Integer occurrenceCount,
        Integer threshold,
        Integer ruleWindowSeconds,
        Integer distinctUserCount,
        Integer distinctDeviceCount,
        Integer distinctIpCount,
        String lastFailureCode,
        Integer maxDistanceMeter,
        List<UUID> sampleAttemptIds,
        List<UUID> sampleUserIds,
        List<String> sampleIpAddresses,
        List<String> sampleDeviceIds,
        List<String> notes,

        String ipAddress,
        String userAgent,
        String deviceId,
        String location,
        Double geoLat,
        Double geoLng,
        Integer distanceMeter,
        Integer allowedRadiusMeter,
        Integer otherUserCount,
        List<UUID> otherUserIds,
        String reason
) {
    public FraudIncidentEvidenceSummaryResponse(
            Integer occurrenceCount,
            Integer threshold,
            Integer ruleWindowSeconds,
            Integer distinctUserCount,
            Integer distinctDeviceCount,
            Integer distinctIpCount,
            String lastFailureCode,
            Integer maxDistanceMeter,
            List<UUID> sampleAttemptIds,
            List<UUID> sampleUserIds,
            List<String> sampleIpAddresses,
            List<String> sampleDeviceIds,
            List<String> notes
    ) {
        this(
                occurrenceCount,
                threshold,
                ruleWindowSeconds,
                distinctUserCount,
                distinctDeviceCount,
                distinctIpCount,
                lastFailureCode,
                maxDistanceMeter,
                sampleAttemptIds,
                sampleUserIds,
                sampleIpAddresses,
                sampleDeviceIds,
                notes,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                null
        );
    }
}
