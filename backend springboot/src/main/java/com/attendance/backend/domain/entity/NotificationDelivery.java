package com.attendance.backend.domain.entity;

import com.attendance.backend.common.persistence.MysqlUuidBinary16SwapType;
import com.attendance.backend.common.persistence.UuidBinary16SwapConverter;
import com.attendance.backend.domain.enums.NotificationChannel;
import com.attendance.backend.domain.enums.NotificationDeliveryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "notification_deliveries",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_notification_deliveries_notification_channel",
                        columnNames = {"notification_id", "channel"}
                )
        },
        indexes = {
                @Index(name = "idx_nd_claim", columnList = "channel, status, next_attempt_at, email_outbox_id, locked_at"),
                @Index(name = "idx_nd_admin_status_created", columnList = "status, created_at"),
                @Index(name = "idx_nd_email_outbox", columnList = "email_outbox_id"),
                @Index(name = "idx_nd_notification_created", columnList = "notification_id, created_at")
        }
)
public class NotificationDelivery {

    @Id
    @Type(value = MysqlUuidBinary16SwapType.class)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID id;

    @Column(name = "notification_id", columnDefinition = "BINARY(16)", nullable = false)
    @Convert(converter = UuidBinary16SwapConverter.class)
    private UUID notificationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationDeliveryStatus status;

    @Column(name = "email_outbox_id", columnDefinition = "BINARY(16)")
    @Convert(converter = UuidBinary16SwapConverter.class)
    private UUID emailOutboxId;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "last_error_code", length = 50)
    private String lastErrorCode;

    @Column(name = "last_error_message", length = 500)
    private String lastErrorMessage;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    public NotificationDelivery() {
    }

    public void markProcessing(Instant now) {
        this.status = NotificationDeliveryStatus.PROCESSING;
        this.lockedAt = now;
    }

    public void markEnqueued(UUID outboxId) {
        this.status = NotificationDeliveryStatus.ENQUEUED;
        this.emailOutboxId = outboxId;
        this.lockedAt = null;
    }

    public void markRetry(String code, String message, Instant nextAttemptAt) {
        this.status = NotificationDeliveryStatus.RETRY;
        this.retryCount += 1;
        this.lastErrorCode = code;
        this.lastErrorMessage = message;
        this.nextAttemptAt = nextAttemptAt;
        this.lockedAt = null;
    }

    public void markDead(String code, String message, Instant now) {
        this.status = NotificationDeliveryStatus.DEAD;
        this.lastErrorCode = code;
        this.lastErrorMessage = message;
        this.lockedAt = null;
        this.processedAt = now;
    }

    public void markDelivered(Instant now) {
        this.status = NotificationDeliveryStatus.DELIVERED;
        this.lockedAt = null;
        this.processedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(UUID notificationId) {
        this.notificationId = notificationId;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public void setChannel(NotificationChannel channel) {
        this.channel = channel;
    }

    public NotificationDeliveryStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationDeliveryStatus status) {
        this.status = status;
    }

    public UUID getEmailOutboxId() {
        return emailOutboxId;
    }

    public void setEmailOutboxId(UUID emailOutboxId) {
        this.emailOutboxId = emailOutboxId;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public void setNextAttemptAt(Instant nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
    }

    public Instant getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(Instant lockedAt) {
        this.lockedAt = lockedAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public String getLastErrorCode() {
        return lastErrorCode;
    }

    public void setLastErrorCode(String lastErrorCode) {
        this.lastErrorCode = lastErrorCode;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public void setLastErrorMessage(String lastErrorMessage) {
        this.lastErrorMessage = lastErrorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}