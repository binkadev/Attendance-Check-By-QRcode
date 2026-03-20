package com.attendance.backend.notification.dto;

import java.util.List;

public record PageNotificationResponse(
        List<NotificationResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}