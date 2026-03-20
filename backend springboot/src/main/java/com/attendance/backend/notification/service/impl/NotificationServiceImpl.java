package com.attendance.backend.notification.service.impl;

import com.attendance.backend.domain.entity.Notification;
import com.attendance.backend.domain.entity.NotificationDelivery;
import com.attendance.backend.domain.entity.NotificationRuleConfig;
import com.attendance.backend.domain.enums.NotificationChannel;
import com.attendance.backend.domain.enums.NotificationDeliveryStatus;
import com.attendance.backend.notification.repository.NotificationDeliveryRepository;
import com.attendance.backend.notification.repository.NotificationRepository;
import com.attendance.backend.notification.repository.NotificationRuleConfigRepository;
import com.attendance.backend.notification.service.NotificationService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Collection;
import java.util.Locale;
import java.util.UUID;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final int MYSQL_DUPLICATE_KEY_ERROR_CODE = 1062;

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationRuleConfigRepository ruleConfigRepository;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            NotificationDeliveryRepository deliveryRepository,
            NotificationRuleConfigRepository ruleConfigRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.deliveryRepository = deliveryRepository;
        this.ruleConfigRepository = ruleConfigRepository;
    }

    @Override
    @Transactional
    public void createOne(CreateNotificationCommand cmd) {
        NotificationRuleConfig inAppRule = ruleConfigRepository.findEffectiveRule(
                cmd.groupId(),
                cmd.type().name(),
                NotificationChannel.IN_APP.name()
        );

        if (!isRuleEnabled(inAppRule)) {
            return;
        }

        if (cmd.dedupKey() != null && notificationRepository.findByDedupKey(cmd.dedupKey()).isPresent()) {
            return;
        }

        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setRecipientUserId(cmd.recipientUserId());
        notification.setGroupId(cmd.groupId());
        notification.setSessionId(cmd.sessionId());
        notification.setType(cmd.type());
        notification.setTitle(cmd.title());
        notification.setBody(cmd.body());
        notification.setPayloadJson(cmd.payloadJson());
        notification.setSeverity(cmd.severity());
        notification.setSourceType(cmd.sourceType());
        notification.setSourceRefId(cmd.sourceRefId());
        notification.setDedupKey(cmd.dedupKey());
        notification.setRead(false);

        try {
            notificationRepository.saveAndFlush(notification);
        } catch (DataIntegrityViolationException ex) {
            if (cmd.dedupKey() != null && isDuplicateDedupKeyViolation(ex)) {
                return;
            }
            throw ex;
        }

        NotificationRuleConfig emailRule = ruleConfigRepository.findEffectiveRule(
                cmd.groupId(),
                cmd.type().name(),
                NotificationChannel.EMAIL.name()
        );

        if (isRuleEnabled(emailRule)) {
            NotificationDelivery delivery = new NotificationDelivery();
            delivery.setId(UUID.randomUUID());
            delivery.setNotificationId(notification.getId());
            delivery.setChannel(NotificationChannel.EMAIL);
            delivery.setStatus(NotificationDeliveryStatus.PENDING);
            delivery.setRetryCount(0);
            delivery.setNextAttemptAt(Instant.now());
            deliveryRepository.save(delivery);
        }
    }

    @Override
    @Transactional
    public void createMany(Collection<CreateNotificationCommand> commands) {
        for (CreateNotificationCommand cmd : commands) {
            createOne(cmd);
        }
    }

    private boolean isRuleEnabled(NotificationRuleConfig rule) {
        return rule == null || rule.isEnabled();
    }

    private boolean isDuplicateDedupKeyViolation(DataIntegrityViolationException ex) {
        Throwable cursor = ex;
        while (cursor != null) {
            if (cursor instanceof SQLException sqlEx) {
                if (sqlEx.getErrorCode() == MYSQL_DUPLICATE_KEY_ERROR_CODE) {
                    String message = safeLower(sqlEx.getMessage());
                    if (message.contains("uk_notifications_dedup_key")
                            || (message.contains("duplicate") && message.contains("dedup_key"))) {
                        return true;
                    }
                }
            }

            String message = safeLower(cursor.getMessage());
            if (message.contains("uk_notifications_dedup_key")
                    || (message.contains("duplicate") && message.contains("dedup_key"))) {
                return true;
            }

            cursor = cursor.getCause();
        }

        return false;
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}