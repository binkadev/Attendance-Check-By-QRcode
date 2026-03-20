package com.attendance.backend.notification.service.impl;

import com.attendance.backend.domain.entity.Notification;
import com.attendance.backend.domain.entity.NotificationDelivery;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class NotificationEmailOutboxBridge {

    public UUID enqueue(Notification notification, NotificationDelivery delivery) {
        // TODO:
        // 1. resolve recipient email từ recipient_user_id
        // 2. build email subject/body/payload
        // 3. insert email_outbox row với:
        //    aggregate_type = "NOTIFICATION_DELIVERY"
        //    aggregate_id   = delivery.id
        // 4. return email_outbox.id

        // tạm skeleton:
        return UUID.randomUUID();
    }
}