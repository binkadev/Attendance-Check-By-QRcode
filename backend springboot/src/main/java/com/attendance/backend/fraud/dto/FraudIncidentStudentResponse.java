package com.attendance.backend.fraud.dto;

import java.util.UUID;

public record FraudIncidentStudentResponse(
        UUID id,
        String studentCode,
        String name,
        String email,
        String avatar
) {
}
