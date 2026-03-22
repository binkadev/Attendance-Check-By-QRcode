package com.attendance.backend.fraud.dto;

import java.util.List;

public record PageFraudIncidentResponse(
    List<FraudIncidentResponse> items,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
}
