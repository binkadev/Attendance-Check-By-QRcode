package com.attendance.backend.fraud.dto;

import com.attendance.backend.fraud.domain.FraudIncidentSeverity;
import com.attendance.backend.fraud.domain.FraudIncidentStatus;
import com.attendance.backend.fraud.domain.FraudIncidentType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FraudIncidentResponse(
        UUID id,
        UUID groupId,
        UUID sessionId,
        UUID userId,
        FraudIncidentType type,
        FraudIncidentSeverity severity,
        FraudIncidentStatus status,
        String displayStatus,
        String title,
        String description,
        Integer confidence,
        FraudIncidentStudentResponse student,
        Instant firstDetectedAt,
        Instant lastDetectedAt,
        Integer occurrenceCount,
        FraudIncidentEvidenceSummaryResponse evidenceSummary,
        UUID assignedToUserId,
        UUID lastActionByUserId,
        Instant resolvedAt,
        String resolutionNote,
        Instant createdAt,
        Instant updatedAt
) {

    public FraudIncidentResponse(
            UUID id,
            UUID groupId,
            UUID sessionId,
            UUID userId,
            FraudIncidentType type,
            FraudIncidentSeverity severity,
            FraudIncidentStatus status,
            Instant firstDetectedAt,
            Instant lastDetectedAt,
            Integer occurrenceCount,
            FraudIncidentEvidenceSummaryResponse evidenceSummary,
            UUID assignedToUserId,
            UUID lastActionByUserId,
            Instant resolvedAt,
            String resolutionNote,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(
                id,
                groupId,
                sessionId,
                userId,
                type,
                severity,
                status,
                null,
                null,
                null,
                null,
                null,
                firstDetectedAt,
                lastDetectedAt,
                occurrenceCount,
                evidenceSummary,
                assignedToUserId,
                lastActionByUserId,
                resolvedAt,
                resolutionNote,
                createdAt,
                updatedAt
        );
    }
}
