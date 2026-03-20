package com.attendance.backend.notification.repository;

import com.attendance.backend.domain.entity.NotificationDelivery;

import java.time.Instant;
import java.util.List;

public interface NotificationDeliveryClaimRepository {
    List<NotificationDelivery> claimDueEmailDeliveries(Instant now, Instant staleBefore, int limit);
}