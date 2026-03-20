package com.attendance.backend.domain.entity;

import com.attendance.backend.common.persistence.MysqlUuidBinary16SwapType;
import com.attendance.backend.common.persistence.UuidBinary16SwapConverter;
import com.attendance.backend.domain.enums.NotificationSeverity;
import com.attendance.backend.domain.enums.NotificationSourceType;
import com.attendance.backend.domain.enums.NotificationType;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notifications_recipient_created", columnList = "recipient_user_id, created_at"),
                @Index(name = "idx_notifications_recipient_unread_created", columnList = "recipient_user_id, is_read, created_at"),
                @Index(name = "idx_notifications_group_recipient_created", columnList = "group_id, recipient_user_id, created_at"),
                @Index(name = "idx_notifications_source", columnList = "source_type, source_ref_id"),
                @Index(name = "idx_notifications_session_created", columnList = "session_id, created_at")
        }
)
public class Notification {

    @Id
    @Type(value = MysqlUuidBinary16SwapType.class)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID id;

    @Column(name = "recipient_user_id", columnDefinition = "BINARY(16)", nullable = false)
    @Convert(converter = UuidBinary16SwapConverter.class)
    private UUID recipientUserId;

    @Column(name = "group_id", columnDefinition = "BINARY(16)")
    @Convert(converter = UuidBinary16SwapConverter.class)
    private UUID groupId;

    @Column(name = "session_id", columnDefinition = "BINARY(16)")
    @Convert(converter = UuidBinary16SwapConverter.class)
    private UUID sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "body", nullable = false, length = 1000)
    private String body;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", columnDefinition = "json")
    private JsonNode payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private NotificationSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 50)
    private NotificationSourceType sourceType;

    @Column(name = "source_ref_id", columnDefinition = "BINARY(16)")
    @Convert(converter = UuidBinary16SwapConverter.class)
    private UUID sourceRefId;

    @Column(name = "dedup_key", length = 64)
    private String dedupKey;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    public Notification() {
    }

    public void markRead(Instant at) {
        if (!this.isRead) {
            this.isRead = true;
            this.readAt = at;
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRecipientUserId() {
        return recipientUserId;
    }

    public void setRecipientUserId(UUID recipientUserId) {
        this.recipientUserId = recipientUserId;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public void setGroupId(UUID groupId) {
        this.groupId = groupId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public JsonNode getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(JsonNode payloadJson) {
        this.payloadJson = payloadJson;
    }

    public NotificationSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(NotificationSeverity severity) {
        this.severity = severity;
    }

    public NotificationSourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(NotificationSourceType sourceType) {
        this.sourceType = sourceType;
    }

    public UUID getSourceRefId() {
        return sourceRefId;
    }

    public void setSourceRefId(UUID sourceRefId) {
        this.sourceRefId = sourceRefId;
    }

    public String getDedupKey() {
        return dedupKey;
    }

    public void setDedupKey(String dedupKey) {
        this.dedupKey = dedupKey;
    }

    public boolean isRead() {
        return isRead;
    }

    public boolean getRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
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