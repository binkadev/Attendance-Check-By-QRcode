package com.attendance.backend.notification.dto;

import java.util.List;

public record PageNotificationDeliveryAdminResponse(
        List<NotificationDeliveryAdminResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}