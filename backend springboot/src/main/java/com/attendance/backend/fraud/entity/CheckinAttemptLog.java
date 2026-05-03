package com.attendance.backend.fraud.entity;

import com.attendance.backend.common.db.MysqlOrderedUuidConverter;
import com.attendance.backend.fraud.domain.CheckinAttemptOutcome;
import com.attendance.backend.fraud.domain.CheckinFailureCode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "checkin_attempt_logs", indexes = {
        @Index(name = "idx_cal_created_at", columnList = "created_at"),
        @Index(name = "idx_cal_session_created", columnList = "session_id, created_at"),
        @Index(name = "idx_cal_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_cal_group_device_created", columnList = "group_id, device_id, created_at"),
        @Index(name = "idx_cal_group_ip_created", columnList = "group_id, ip_address, created_at"),
        @Index(name = "idx_cal_outcome_code_created", columnList = "outcome, failure_code, created_at"),
        @Index(name = "idx_cal_token_hash_created", columnList = "token_hash, created_at")
})
public class CheckinAttemptLog {

    @Id
    @Convert(converter = MysqlOrderedUuidConverter.class)
    @Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Convert(converter = MysqlOrderedUuidConverter.class)
    @Column(name = "group_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID groupId;

    @Convert(converter = MysqlOrderedUuidConverter.class)
    @Column(name = "session_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID sessionId;

    @Convert(converter = MysqlOrderedUuidConverter.class)
    @Column(name = "user_id", columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(name = "qr_token_id", length = 64)
    private String qrTokenId;

    @Column(name = "token_hash")
    private byte[] tokenHash;

    @Column(name = "device_id", length = 120)
    private String deviceId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "geo_lat", precision = 10, scale = 7)
    private BigDecimal geoLat;

    @Column(name = "geo_lng", precision = 10, scale = 7)
    private BigDecimal geoLng;

    @Column(name = "distance_meter")
    private Integer distanceMeter;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 32)
    private CheckinAttemptOutcome outcome;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_code", length = 40)
    private CheckinFailureCode failureCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json")
    private String payloadJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getGroupId() { return groupId; }
    public void setGroupId(UUID groupId) { this.groupId = groupId; }
    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getQrTokenId() { return qrTokenId; }
    public void setQrTokenId(String qrTokenId) { this.qrTokenId = qrTokenId; }
    public byte[] getTokenHash() { return tokenHash; }
    public void setTokenHash(byte[] tokenHash) { this.tokenHash = tokenHash; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public BigDecimal getGeoLat() { return geoLat; }
    public void setGeoLat(BigDecimal geoLat) { this.geoLat = geoLat; }
    public BigDecimal getGeoLng() { return geoLng; }
    public void setGeoLng(BigDecimal geoLng) { this.geoLng = geoLng; }
    public Integer getDistanceMeter() { return distanceMeter; }
    public void setDistanceMeter(Integer distanceMeter) { this.distanceMeter = distanceMeter; }
    public CheckinAttemptOutcome getOutcome() { return outcome; }
    public void setOutcome(CheckinAttemptOutcome outcome) { this.outcome = outcome; }
    public CheckinFailureCode getFailureCode() { return failureCode; }
    public void setFailureCode(CheckinFailureCode failureCode) { this.failureCode = failureCode; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}