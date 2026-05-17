package com.attendance.backend.fraud.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FraudIncidentStudentResponse(
        UUID id,
        String studentCode,
        String name,
        String email,
        String avatar
) {
}
