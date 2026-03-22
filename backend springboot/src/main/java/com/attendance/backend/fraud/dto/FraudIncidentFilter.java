package com.attendance.backend.fraud.dto;

import com.attendance.backend.fraud.domain.FraudIncidentSeverity;
import com.attendance.backend.fraud.domain.FraudIncidentStatus;
import com.attendance.backend.fraud.domain.FraudIncidentType;
import org.springframework.data.domain.Sort;

import java.util.Set;
import java.util.UUID;

public record FraudIncidentFilter(
    Set<FraudIncidentStatus> statuses,
    Set<FraudIncidentType> types,
    Set<FraudIncidentSeverity> severities,
    UUID assignedToUserId,
    FraudIncidentSortBy sortBy,
    Sort.Direction sortDir
) {
    public FraudIncidentSortBy normalizedSortBy() {
        return sortBy == null ? FraudIncidentSortBy.LAST_DETECTED_AT : sortBy;
    }

    public Sort.Direction normalizedSortDir() {
        return sortDir == null ? Sort.Direction.DESC : sortDir;
    }
}
