package com.attendance.backend.fraud.dto;

import com.attendance.backend.fraud.domain.PatchFraudIncidentAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record PatchFraudIncidentRequest(
    @NotNull PatchFraudIncidentAction action,
    @Size(max = 500) String note,
    UUID assignedToUserId
) {
}
