package com.attendance.backend.notification.service.impl;

import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.domain.entity.Notification;
import com.attendance.backend.domain.entity.NotificationDelivery;
import com.attendance.backend.domain.enums.NotificationChannel;
import com.attendance.backend.domain.enums.NotificationDeliveryStatus;
import com.attendance.backend.domain.enums.NotificationSeverity;
import com.attendance.backend.domain.enums.NotificationType;
import com.attendance.backend.notification.dto.NotificationDeliveryAdminResponse;
import com.attendance.backend.notification.dto.NotificationReadAllResponse;
import com.attendance.backend.notification.dto.NotificationResponse;
import com.attendance.backend.notification.dto.NotificationUnreadCountResponse;
import com.attendance.backend.notification.dto.PageNotificationDeliveryAdminResponse;
import com.attendance.backend.notification.dto.PageNotificationResponse;
import com.attendance.backend.notification.repository.NotificationDeliveryRepository;
import com.attendance.backend.notification.repository.NotificationRepository;
import com.attendance.backend.notification.service.NotificationQueryService;
import com.attendance.backend.notification.service.mapper.NotificationMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NotificationQueryServiceImpl implements NotificationQueryService {

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final NotificationMapper notificationMapper;
    private final Clock clock;

    public NotificationQueryServiceImpl(
            NotificationRepository notificationRepository,
            NotificationDeliveryRepository notificationDeliveryRepository,
            NotificationMapper notificationMapper,
            Clock clock
    ) {
        this.notificationRepository = notificationRepository;
        this.notificationDeliveryRepository = notificationDeliveryRepository;
        this.notificationMapper = notificationMapper;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PageNotificationResponse getMyNotifications(
            UUID currentUserId,
            int page,
            int size,
            boolean unreadOnly,
            String type,
            String severity
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));

        NotificationType parsedType = parseNotificationType(type);
        NotificationSeverity parsedSeverity = parseNotificationSeverity(severity);

        var result = notificationRepository.findMyNotifications(
                currentUserId,
                unreadOnly,
                parsedType,
                parsedSeverity,
                PageRequest.of(safePage, safeSize)
        );

        return new PageNotificationResponse(
                result.getContent().stream().map(notificationMapper::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationUnreadCountResponse getUnreadCount(UUID currentUserId) {
        return new NotificationUnreadCountResponse(
                notificationRepository.countUnread(currentUserId)
        );
    }

    @Override
    @Transactional
    public NotificationResponse markRead(UUID currentUserId, UUID notificationId) {
        Instant now = Instant.now(clock);

        notificationRepository.markRead(notificationId, currentUserId, now);

        Notification notification = notificationRepository.findByIdAndRecipientUserId(notificationId, currentUserId)
                .orElseThrow(() -> ApiException.notFound(
                        "NOTIFICATION_NOT_FOUND",
                        "Notification not found"
                ));

        return notificationMapper.toResponse(notification);
    }

    @Override
    @Transactional
    public NotificationReadAllResponse markReadAll(UUID currentUserId) {
        Instant now = Instant.now(clock);
        int updated = notificationRepository.markReadAll(currentUserId, now);
        return new NotificationReadAllResponse(updated, now);
    }

    @Override
    @Transactional(readOnly = true)
    public PageNotificationResponse getLecturerGroupNotifications(
            UUID currentUserId,
            UUID groupId,
            int page,
            int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));

        var result = notificationRepository.findGroupNotifications(
                currentUserId,
                groupId,
                PageRequest.of(safePage, safeSize)
        );

        return new PageNotificationResponse(
                result.getContent().stream().map(notificationMapper::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageNotificationDeliveryAdminResponse getAdminDeliveries(
            int page,
            int size,
            String channel,
            Set<String> statuses
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));

        NotificationChannel parsedChannel = parseNotificationChannel(channel);
        Set<NotificationDeliveryStatus> parsedStatuses = parseDeliveryStatuses(statuses);

        var result = notificationDeliveryRepository.findByChannelAndStatusIn(
                parsedChannel,
                parsedStatuses,
                PageRequest.of(safePage, safeSize)
        );

        List<NotificationDeliveryAdminResponse> items = new ArrayList<>();
        for (NotificationDelivery delivery : result.getContent()) {
            Notification notification = notificationRepository.findById(delivery.getNotificationId())
                    .orElseThrow(() -> ApiException.notFound(
                            "NOTIFICATION_NOT_FOUND",
                            "Notification not found"
                    ));
            items.add(notificationMapper.toAdminResponse(delivery, notification));
        }

        return new PageNotificationDeliveryAdminResponse(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private NotificationType parseNotificationType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            return NotificationType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest(
                    "INVALID_NOTIFICATION_TYPE",
                    "Invalid notification type"
            );
        }
    }

    private NotificationSeverity parseNotificationSeverity(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            return NotificationSeverity.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest(
                    "INVALID_NOTIFICATION_SEVERITY",
                    "Invalid notification severity"
            );
        }
    }

    private NotificationChannel parseNotificationChannel(String raw) {
        if (raw == null || raw.isBlank()) {
            return NotificationChannel.EMAIL;
        }

        try {
            return NotificationChannel.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest(
                    "INVALID_NOTIFICATION_CHANNEL",
                    "Invalid notification channel"
            );
        }
    }

    private Set<NotificationDeliveryStatus> parseDeliveryStatuses(Set<String> rawStatuses) {
        if (rawStatuses == null || rawStatuses.isEmpty()) {
            return EnumSet.of(
                    NotificationDeliveryStatus.RETRY,
                    NotificationDeliveryStatus.DEAD
            );
        }

        try {
            return rawStatuses.stream()
                    .filter(s -> s != null && !s.isBlank())
                    .map(s -> NotificationDeliveryStatus.valueOf(s.trim().toUpperCase(Locale.ROOT)))
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(NotificationDeliveryStatus.class)));
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest(
                    "INVALID_NOTIFICATION_DELIVERY_STATUS",
                    "Invalid notification delivery status"
            );
        }
    }
}