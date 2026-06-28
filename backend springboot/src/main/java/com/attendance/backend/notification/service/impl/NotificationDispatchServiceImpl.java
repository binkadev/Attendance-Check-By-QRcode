package com.attendance.backend.notification.service.impl;

import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.domain.entity.Notification;
import com.attendance.backend.domain.entity.NotificationDelivery;
import com.attendance.backend.domain.enums.NotificationDeliveryStatus;
import com.attendance.backend.mail.EmailOutbox;
import com.attendance.backend.mail.EmailOutboxRepository;
import com.attendance.backend.notification.repository.NotificationDeliveryRepository;
import com.attendance.backend.notification.repository.NotificationRepository;
import com.attendance.backend.notification.service.NotificationDispatchService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationDispatchServiceImpl implements NotificationDispatchService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long[] BACKOFF_MINUTES = {1, 5, 15, 60};

    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationEmailOutboxBridge notificationEmailOutboxBridge;
    private final EmailOutboxRepository emailOutboxRepository;
    private final Clock clock;

    public NotificationDispatchServiceImpl(
            NotificationDeliveryRepository notificationDeliveryRepository,
            NotificationRepository notificationRepository,
            NotificationEmailOutboxBridge notificationEmailOutboxBridge,
            EmailOutboxRepository emailOutboxRepository,
            Clock clock
    ) {
        this.notificationDeliveryRepository = notificationDeliveryRepository;
        this.notificationRepository = notificationRepository;
        this.notificationEmailOutboxBridge = notificationEmailOutboxBridge;
        this.emailOutboxRepository = emailOutboxRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public int dispatchDueEmailDeliveries(int batchSize) {
        Instant now = Instant.now(clock);

        List<NotificationDelivery> due = notificationDeliveryRepository.claimDueEmailDeliveries(
                now,
                now.minus(Duration.ofMinutes(5)),
                batchSize
        );

        int processed = 0;
        for (NotificationDelivery delivery : due) {
            processed += processOne(delivery.getId()) ? 1 : 0;
        }
        return processed;
    }

    @Transactional
    protected boolean processOne(UUID deliveryId) {
        Instant now = Instant.now(clock);

        NotificationDelivery delivery = notificationDeliveryRepository.findById(deliveryId)
                .orElseThrow(() -> ApiException.notFound(
                        "NOTIFICATION_DELIVERY_NOT_FOUND",
                        "Notification delivery not found"
                ));

        Notification notification = notificationRepository.findById(delivery.getNotificationId())
                .orElseThrow(() -> ApiException.notFound(
                        "NOTIFICATION_NOT_FOUND",
                        "Notification not found"
                ));

        try {
            delivery.markProcessing(now);
            notificationDeliveryRepository.save(delivery);

            UUID outboxId = notificationEmailOutboxBridge.enqueue(notification, delivery);
            if (outboxId == null || !emailOutboxRepository.existsById(outboxId)) {
                throw new IllegalStateException("Notification email outbox row was not persisted");
            }

            delivery.markEnqueued(outboxId);
            notificationDeliveryRepository.save(delivery);
            return true;
        } catch (Exception ex) {
            String errorMessage = trimError(ex.getMessage());

            if (delivery.getRetryCount() + 1 >= MAX_ATTEMPTS) {
                delivery.markDead("DISPATCH_FAILED", errorMessage, now);
            } else {
                long waitMinutes = BACKOFF_MINUTES[Math.min(
                        delivery.getRetryCount(),
                        BACKOFF_MINUTES.length - 1
                )];
                delivery.markRetry(
                        "DISPATCH_FAILED",
                        errorMessage,
                        now.plus(Duration.ofMinutes(waitMinutes))
                );
            }

            notificationDeliveryRepository.save(delivery);
            return false;
        }
    }

    @Override
    @Transactional
    public void retryDelivery(UUID deliveryId) {
        Instant now = Instant.now(clock);

        NotificationDelivery delivery = notificationDeliveryRepository.findById(deliveryId)
                .orElseThrow(() -> ApiException.notFound(
                        "NOTIFICATION_DELIVERY_NOT_FOUND",
                        "Notification delivery not found"
                ));

        if (delivery.getStatus() == NotificationDeliveryStatus.DELIVERED) {
            throw ApiException.conflict(
                    "NOTIFICATION_DELIVERY_ALREADY_DELIVERED",
                    "Delivered notification cannot be retried"
            );
        }

        if (delivery.getStatus() == NotificationDeliveryStatus.PROCESSING) {
            throw ApiException.conflict(
                    "NOTIFICATION_DELIVERY_PROCESSING",
                    "Notification delivery is currently processing"
            );
        }

        if (delivery.getEmailOutboxId() == null) {
            delivery.setStatus(NotificationDeliveryStatus.PENDING);
            delivery.setRetryCount(0);
            delivery.setNextAttemptAt(now);
            delivery.setLockedAt(null);
            delivery.setProcessedAt(null);
            delivery.setLastErrorCode(null);
            delivery.setLastErrorMessage(null);
            notificationDeliveryRepository.save(delivery);
            return;
        }

        EmailOutbox outbox = emailOutboxRepository.findByIdForUpdate(delivery.getEmailOutboxId())
                .orElseThrow(() -> ApiException.notFound(
                        "EMAIL_OUTBOX_NOT_FOUND",
                        "Email outbox row not found"
                ));

        if (EmailOutbox.STATUS_SENT.equals(outbox.status)) {
            throw ApiException.conflict(
                    "EMAIL_OUTBOX_ALREADY_SENT",
                    "Outbox row already sent"
            );
        }

        if (EmailOutbox.STATUS_PROCESSING.equals(outbox.status)) {
            throw ApiException.conflict(
                    "EMAIL_OUTBOX_PROCESSING",
                    "Outbox row is currently processing"
            );
        }

        outbox.status = EmailOutbox.STATUS_PENDING;
        outbox.retryCount = 0;
        outbox.nextAttemptAt = now;
        outbox.lockedAt = null;
        outbox.processedAt = null;
        outbox.lastErrorCode = null;
        outbox.lastErrorMessage = null;
        emailOutboxRepository.save(outbox);

        delivery.setStatus(NotificationDeliveryStatus.ENQUEUED);
        delivery.setLockedAt(null);
        delivery.setProcessedAt(null);
        delivery.setLastErrorCode(null);
        delivery.setLastErrorMessage(null);
        notificationDeliveryRepository.save(delivery);
    }

    private String trimError(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Unknown dispatch error";
        }
        return raw.length() <= 500 ? raw : raw.substring(0, 500);
    }
}
