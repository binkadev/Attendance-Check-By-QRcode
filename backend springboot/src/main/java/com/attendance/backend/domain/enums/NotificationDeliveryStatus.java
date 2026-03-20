package com.attendance.backend.domain.enums;

public enum NotificationDeliveryStatus {
    PENDING,
    PROCESSING,
    ENQUEUED,
    RETRY,
    DELIVERED,
    DEAD
}