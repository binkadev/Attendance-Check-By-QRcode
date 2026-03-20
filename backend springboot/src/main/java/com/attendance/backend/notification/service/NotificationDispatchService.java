package com.attendance.backend.notification.service;

import java.util.UUID;

public interface NotificationDispatchService {
    int dispatchDueEmailDeliveries(int batchSize);
    void retryDelivery(UUID deliveryId);
}