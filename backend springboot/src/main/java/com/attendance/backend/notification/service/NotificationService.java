package com.attendance.backend.notification.service;

import com.attendance.backend.domain.enums.NotificationSeverity;
import com.attendance.backend.domain.enums.NotificationSourceType;
import com.attendance.backend.domain.enums.NotificationType;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collection;
import java.util.UUID;

public interface NotificationService {

    record CreateNotificationCommand(
            UUID recipientUserId,
            UUID groupId,
            UUID sessionId,
            NotificationType type,
            String title,
            String body,
            JsonNode payloadJson,
            NotificationSeverity severity,
            NotificationSourceType sourceType,
            UUID sourceRefId,
            String dedupKey
    ) {}

    void createOne(CreateNotificationCommand command);

    void createMany(Collection<CreateNotificationCommand> commands);
}