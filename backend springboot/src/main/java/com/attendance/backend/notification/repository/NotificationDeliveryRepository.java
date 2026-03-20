package com.attendance.backend.notification.repository;

import com.attendance.backend.domain.entity.NotificationDelivery;
import com.attendance.backend.domain.enums.NotificationChannel;
import com.attendance.backend.domain.enums.NotificationDeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationDeliveryRepository
        extends JpaRepository<NotificationDelivery, UUID>, NotificationDeliveryClaimRepository {

    Page<NotificationDelivery> findByChannelAndStatusIn(
            NotificationChannel channel,
            Collection<NotificationDeliveryStatus> statuses,
            Pageable pageable
    );

    Optional<NotificationDelivery> findById(UUID id);

    Optional<NotificationDelivery> findByNotificationIdAndChannel(UUID notificationId, NotificationChannel channel);

    List<NotificationDelivery> findByEmailOutboxIdIn(Collection<UUID> emailOutboxIds);
}