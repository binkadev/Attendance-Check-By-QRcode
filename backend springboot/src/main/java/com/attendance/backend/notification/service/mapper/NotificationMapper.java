package com.attendance.backend.notification.service.mapper;

import com.attendance.backend.domain.entity.Notification;
import com.attendance.backend.domain.entity.NotificationDelivery;
import com.attendance.backend.notification.dto.NotificationDeliveryAdminResponse;
import com.attendance.backend.notification.dto.NotificationResponse;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getGroupId(),
                n.getSessionId(),
                n.getType(),
                n.getTitle(),
                n.getBody(),
                n.getPayloadJson(),
                n.getSeverity(),
                n.getSourceType(),
                n.getSourceRefId(),
                n.isRead(),
                n.getReadAt(),
                n.getCreatedAt()
        );
    }

    public NotificationDeliveryAdminResponse toAdminResponse(NotificationDelivery d, Notification n) {
        return new NotificationDeliveryAdminResponse(
                d.getId(),
                d.getNotificationId(),
                n.getRecipientUserId(),
                n.getGroupId(),
                n.getType(),
                d.getChannel(),
                d.getStatus(),
                d.getEmailOutboxId(),
                d.getRetryCount(),
                d.getNextAttemptAt(),
                d.getLockedAt(),
                d.getProcessedAt(),
                d.getLastErrorCode(),
                d.getLastErrorMessage(),
                d.getCreatedAt(),
                d.getUpdatedAt()
        );
    }
}