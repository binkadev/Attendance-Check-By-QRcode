package com.attendance.backend.fraud.repository;

import com.attendance.backend.common.db.MysqlOrderedUuidConverter;
import com.attendance.backend.fraud.domain.FraudIncidentSeverity;
import com.attendance.backend.fraud.domain.FraudIncidentStatus;
import com.attendance.backend.fraud.domain.FraudIncidentType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fraud_incidents", indexes = {
        @Index(name = "idx_fi_group_status_last_detected", columnList = "group_id, status, last_detected_at"),
        @Index(name = "idx_fi_group_type_last_detected", columnList = "group_id, type, last_detected_at"),
        @Index(name = "idx_fi_group_severity_last_detected", columnList = "group_id, severity, last_detected_at"),
        @Index(name = "idx_fi_assigned_status_last_detected", columnList = "assigned_to_user_id, status, last_detected_at"),
        @Index(name = "idx_fi_group_created", columnList = "group_id, created_at")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_fraud_incidents_dedup_key", columnNames = "dedup_key")
})
public class FraudIncident {

    @Id
    @Convert(converter = MysqlOrderedUuidConverter.class)
    @Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Convert(converter = MysqlOrderedUuidConverter.class)
    @Column(name = "group_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID groupId;

    @Convert(converter = MysqlOrderedUuidConverter.class)
    @Column(name = "session_id", columnDefinition = "BINARY(16)")
    private UUID sessionId;

    @Convert(converter = MysqlOrderedUuidConverter.class)
    @Column(name = "user_id", columnDefinition = "BINARY(16)")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private FraudIncidentType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private FraudIncidentSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FraudIncidentStatus status;

    @Column(name = "dedup_key", nullable = false, length = 64)
    private String dedupKey;

    @Column(name = "first_detected_at", nullable = false)
    private Instant firstDetectedAt;

    @Column(name = "last_detected_at", nullable = false)
    private Instant lastDetectedAt;

    @Column(name = "occurrence_count", nullable = false)
    private Integer occurrenceCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence_json")
    private String evidenceJson;

    @Convert(converter = MysqlOrderedUuidConverter.class)
    @Column(name = "assigned_to_user_id", columnDefinition = "BINARY(16)")
    private UUID assignedToUserId;

    @Convert(converter = MysqlOrderedUuidConverter.class)
    @Column(name = "last_action_by_user_id", columnDefinition = "BINARY(16)")
    private UUID lastActionByUserId;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolution_note", length = 500)
    private String resolutionNote;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getGroupId() { return groupId; }
    public void setGroupId(UUID groupId) { this.groupId = groupId; }
    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public FraudIncidentType getType() { return type; }
    public void setType(FraudIncidentType type) { this.type = type; }
    public FraudIncidentSeverity getSeverity() { return severity; }
    public void setSeverity(FraudIncidentSeverity severity) { this.severity = severity; }
    public FraudIncidentStatus getStatus() { return status; }
    public void setStatus(FraudIncidentStatus status) { this.status = status; }
    public String getDedupKey() { return dedupKey; }
    public void setDedupKey(String dedupKey) { this.dedupKey = dedupKey; }
    public Instant getFirstDetectedAt() { return firstDetectedAt; }
    public void setFirstDetectedAt(Instant firstDetectedAt) { this.firstDetectedAt = firstDetectedAt; }
    public Instant getLastDetectedAt() { return lastDetectedAt; }
    public void setLastDetectedAt(Instant lastDetectedAt) { this.lastDetectedAt = lastDetectedAt; }
    public Integer getOccurrenceCount() { return occurrenceCount; }
    public void setOccurrenceCount(Integer occurrenceCount) { this.occurrenceCount = occurrenceCount; }
    public String getEvidenceJson() { return evidenceJson; }
    public void setEvidenceJson(String evidenceJson) { this.evidenceJson = evidenceJson; }
    public UUID getAssignedToUserId() { return assignedToUserId; }
    public void setAssignedToUserId(UUID assignedToUserId) { this.assignedToUserId = assignedToUserId; }
    public UUID getLastActionByUserId() { return lastActionByUserId; }
    public void setLastActionByUserId(UUID lastActionByUserId) { this.lastActionByUserId = lastActionByUserId; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
    public String getResolutionNote() { return resolutionNote; }
    public void setResolutionNote(String resolutionNote) { this.resolutionNote = resolutionNote; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}