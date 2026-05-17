package com.attendance.backend.fraud.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FraudIncidentLocationResponse(
        BigDecimal lat,
        BigDecimal lng
) {
}
