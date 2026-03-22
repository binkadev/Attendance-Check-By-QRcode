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
    List<String> notes
) {
}
