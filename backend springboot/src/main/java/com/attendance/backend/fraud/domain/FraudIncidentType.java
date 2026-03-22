package com.attendance.backend.fraud.domain;

public enum FraudIncidentType {
    REPEATED_FAILED_QR_TOKEN,
    WRONG_SESSION_QR_TOKEN,
    EXPIRED_QR_TOKEN,
    REPEATED_OUT_OF_RANGE,
    IP_BURST_MULTI_ATTEMPT,
    SHARED_DEVICE_MULTI_ACCOUNT
}
