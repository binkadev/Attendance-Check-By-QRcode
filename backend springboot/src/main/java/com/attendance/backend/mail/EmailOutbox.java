package com.attendance.backend.mail;

import com.attendance.backend.common.persistence.MysqlUuidBinary16SwapType;
import com.attendance.backend.common.persistence.UuidBinary16SwapConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "email_outbox",
        indexes = {
                @Index(name = "idx_eo_status_next", columnList = "status, next_attempt_at"),
                @Index(name = "idx_eo_aggregate", columnList = "aggregate_type, aggregate_id"),
                @Index(name = "idx_eo_created", columnList = "created_at")
        }
)
public class EmailOutbox {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_RETRY = "RETRY";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_DEAD = "DEAD";

    public static final String AGGREGATE_TYPE_PASSWORD_RESET = "PASSWORD_RESET";
    public static final String AGGREGATE_TYPE_NOTIFICATION_DELIVERY = "NOTIFICATION_DELIVERY";

    @Id
    @Type(value = MysqlUuidBinary16SwapType.class)
    @Column(name = "id", columnDefinition = "BINARY(16)")
    public UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    public String aggregateType;

    @Column(name = "aggregate_id", columnDefinition = "BINARY(16)")
    @Convert(converter = UuidBinary16SwapConverter.class)
    public UUID aggregateId;

    @Column(name = "to_email", nullable = false, length = 190)
    public String toEmail;

    @Column(name = "subject", nullable = false, length = 200)
    public String subject;

    @Column(name = "payload_nonce", nullable = false, columnDefinition = "VARBINARY(12)")
    public byte[] payloadNonce;

    @Column(name = "payload_ciphertext", nullable = false, columnDefinition = "VARBINARY(4096)")
    public byte[] payloadCiphertext;

    @Column(name = "status", nullable = false, length = 20)
    public String status;

    @Column(name = "retry_count", nullable = false)
    public int retryCount;

    @Column(name = "next_attempt_at", nullable = false)
    public Instant nextAttemptAt;

    @Column(name = "locked_at")
    public Instant lockedAt;

    @Column(name = "processed_at")
    public Instant processedAt;

    @Column(name = "last_error_code", length = 50)
    public String lastErrorCode;

    @Column(name = "last_error_message", length = 500)
    public String lastErrorMessage;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    public Instant updatedAt;

    public void markProcessing(Instant now) {
        this.status = STATUS_PROCESSING;
        this.lockedAt = now;
    }

    public void markSent(Instant now) {
        this.status = STATUS_SENT;
        this.processedAt = now;
        this.lockedAt = null;
        this.lastErrorCode = null;
        this.lastErrorMessage = null;
        this.nextAttemptAt = now;
    }

    public void markRetry(Instant now, long delaySeconds, String errorCode, String errorMessage) {
        this.status = STATUS_RETRY;
        this.retryCount = this.retryCount + 1;
        this.nextAttemptAt = now.plusSeconds(delaySeconds);
        this.lockedAt = null;
        this.lastErrorCode = errorCode;
        this.lastErrorMessage = errorMessage;
    }

    public void markDead(Instant now, String errorCode, String errorMessage) {
        this.status = STATUS_DEAD;
        this.retryCount = this.retryCount + 1;
        this.processedAt = now;
        this.lockedAt = null;
        this.lastErrorCode = errorCode;
        this.lastErrorMessage = errorMessage;
    }
}