package com.attendance.backend.fraud.service;

import com.attendance.backend.fraud.domain.FraudIncidentSeverity;
import com.attendance.backend.fraud.domain.FraudIncidentType;
import com.attendance.backend.fraud.domain.PatchFraudIncidentAction;
import com.attendance.backend.fraud.entity.FraudIncident;

import java.time.Instant;
import java.util.UUID;

public interface FraudIncidentService {

    FraudIncident openOrBump(OpenFraudIncidentCommand command);

    FraudIncident applyAction(UUID groupId, UUID incidentId, UUID actorUserId, PatchFraudIncidentAction action, String note, UUID assignedToUserId);

    record OpenFraudIncidentCommand(
        UUID groupId,
        UUID sessionId,
        UUID userId,
        FraudIncidentType type,
        FraudIncidentSeverity severity,
        String dedupKey,
        Instant detectedAt,
        String evidenceJson
    ) {
    }
}
