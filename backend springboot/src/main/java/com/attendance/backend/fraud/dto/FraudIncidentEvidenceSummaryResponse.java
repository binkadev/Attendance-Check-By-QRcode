package com.attendance.backend.fraud.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record FraudIncidentEvidenceSummaryResponse(
        Integer occurrenceCount,
        String reason,
        String deviceId,
        String ipAddress,
        String userAgent,
        Integer distinctUserCount,
        List<UUID> involvedUserIds,
        List<FraudIncidentStudentResponse> involvedUsers,
        Integer distanceMeter,
        Integer allowedRadiusMeter,
        FraudIncidentLocationResponse location,
        String lastFailureCode
) {
    /**
     * Backward-compatible constructor for old tests/call sites.
     * The response schema remains the new compact schema because old kitchen-sink
     * fields are not record components anymore.
     */
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
                firstString(notes),
                firstString(sampleDeviceIds),
                firstString(sampleIpAddresses),
                null,
                distinctUserCount,
                emptyToNull(sampleUserIds),
                null,
                maxDistanceMeter,
                null,
                null,
                lastFailureCode
        );
    }

    /**
     * Backward-compatible constructor for the previous enriched DTO version.
     * It also maps into the compact schema and does not expose removed
     * kitchen-sink fields.
     */
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
        this(
                occurrenceCount,
                reason != null ? reason : firstString(notes),
                deviceId != null ? deviceId : firstString(sampleDeviceIds),
                ipAddress != null ? ipAddress : firstString(sampleIpAddresses),
                userAgent,
                distinctUserCount != null ? distinctUserCount : plusOne(otherUserCount),
                emptyToNull(otherUserIds != null && !otherUserIds.isEmpty() ? otherUserIds : sampleUserIds),
                null,
                distanceMeter != null ? distanceMeter : maxDistanceMeter,
                allowedRadiusMeter,
                locationFrom(geoLat, geoLng),
                lastFailureCode
        );
    }

    private static String firstString(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        String value = values.get(0);
        return value == null || value.isBlank() ? null : value;
    }

    private static <T> List<T> emptyToNull(List<T> values) {
        return values == null || values.isEmpty() ? null : values;
    }

    private static Integer plusOne(Integer value) {
        return value == null ? null : value + 1;
    }

    private static FraudIncidentLocationResponse locationFrom(Double lat, Double lng) {
        if (lat == null || lng == null) {
            return null;
        }

        return new FraudIncidentLocationResponse(
                java.math.BigDecimal.valueOf(lat),
                java.math.BigDecimal.valueOf(lng)
        );
    }
}
