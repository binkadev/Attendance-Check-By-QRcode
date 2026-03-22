package com.attendance.backend.fraud.repository;

import com.attendance.backend.fraud.domain.CheckinFailureCode;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CheckinAttemptLogRepositoryCustom {

    long countBySessionUserTokenHashFailureCodesWithinWindow(
        UUID sessionId,
        UUID userId,
        byte[] tokenHash,
        Collection<CheckinFailureCode> failureCodes,
        Instant from
    );

    long countBySessionUserFailureCodeWithinWindow(
        UUID sessionId,
        UUID userId,
        CheckinFailureCode failureCode,
        Instant from
    );

    IpBurstAggregate aggregateIpBurst(UUID groupId, String ipAddress, Instant from);

    DeviceMultiAccountAggregate aggregateSharedDevice(UUID groupId, String deviceId, Instant from);

    List<UUID> findRecentAttemptIdsForEvidence(UUID sessionId, UUID userId, Instant from, int limit);

    record IpBurstAggregate(long totalAttempts, long failAttempts) {}
    record DeviceMultiAccountAggregate(long distinctUserCount) {}
}
