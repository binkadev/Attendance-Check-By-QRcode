package com.attendance.backend.notification.service;

import com.attendance.backend.notification.dto.NotificationReadAllResponse;
import com.attendance.backend.notification.dto.NotificationResponse;
import com.attendance.backend.notification.dto.NotificationUnreadCountResponse;
import com.attendance.backend.notification.dto.PageNotificationDeliveryAdminResponse;
import com.attendance.backend.notification.dto.PageNotificationResponse;

import java.util.Set;
import java.util.UUID;

public interface NotificationQueryService {

    PageNotificationResponse getMyNotifications(
            UUID currentUserId,
            int page,
            int size,
            boolean unreadOnly,
            String type,
            String severity
    );

    NotificationUnreadCountResponse getUnreadCount(UUID currentUserId);

    NotificationResponse markRead(UUID currentUserId, UUID notificationId);

    NotificationReadAllResponse markReadAll(UUID currentUserId);

    PageNotificationResponse getLecturerGroupNotifications(
            UUID currentUserId,
            UUID groupId,
            int page,
            int size
    );

    PageNotificationDeliveryAdminResponse getAdminDeliveries(
            int page,
            int size,
            String channel,
            Set<String> statuses
    );
}