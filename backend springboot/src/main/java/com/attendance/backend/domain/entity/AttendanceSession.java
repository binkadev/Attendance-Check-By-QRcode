package com.attendance.backend.domain.entity;

import com.attendance.backend.common.persistence.MysqlUuidBinary16SwapType;
import com.attendance.backend.common.persistence.UuidBinary16SwapConverter;
import com.attendance.backend.domain.enums.SessionStatus;
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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "attendance_sessions",
        indexes = {
                @Index(name = "idx_as_group_date", columnList = "group_id, session_date"),
                @Index(name = "idx_as_group_created", columnList = "group_id, created_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_as_group_single_open", columnNames = {"group_id", "is_open"})
        }
)
public class AttendanceSession {

    @Id
    @Type(value = MysqlUuidBinary16SwapType.class)
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "group_id", columnDefinition = "BINARY(16)", nullable = false)
    @Convert(converter = UuidBinary16SwapConverter.class)
    private UUID groupId;

    @Column(name = "created_by_user_id", columnDefinition = "BINARY(16)", nullable = false)
    @Convert(converter = UuidBinary16SwapConverter.class)
    private UUID createdByUserId;

    @Column(name = "title", length = 150)
    private String title;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at")
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SessionStatus status;

    @Column(name = "is_open", insertable = false, updatable = false)
    private Boolean open;

    @Column(name = "time_window_minutes", nullable = false)
    private int timeWindowMinutes;

    @Column(name = "late_after_minutes", nullable = false)
    private int lateAfterMinutes;

    @Column(name = "qr_rotate_seconds", nullable = false)
    private int qrRotateSeconds;

    @Column(name = "session_secret", nullable = false, length = 255)
    private String sessionSecret;

    @Column(name = "allow_manual_override", nullable = false)
    private byte allowManualOverride;

    @Column(name = "checkin_open_at")
    private Instant checkinOpenAt;

    @Column(name = "checkin_close_at")
    private Instant checkinCloseAt;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public AttendanceSession() {
    }

    public boolean isAllowManualOverride() {
        return allowManualOverride == 1;
    }

    public void setAllowManualOverride(boolean allowManualOverride) {
        this.allowManualOverride = (byte) (allowManualOverride ? 1 : 0);
    }

    public byte getAllowManualOverride() {
        return allowManualOverride;
    }

    public void setAllowManualOverride(byte allowManualOverride) {
        this.allowManualOverride = allowManualOverride;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public void setGroupId(UUID groupId) {
        this.groupId = groupId;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(UUID createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Deprecated
    public String getSessionName() {
        return title;
    }

    @Deprecated
    public void setSessionName(String sessionName) {
        this.title = sessionName;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(LocalDate sessionDate) {
        this.sessionDate = sessionDate;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public void setStartAt(Instant startAt) {
        this.startAt = startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public void setEndAt(Instant endAt) {
        this.endAt = endAt;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public boolean isOpen() {
        return Boolean.TRUE.equals(open);
    }

    public Boolean getOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public int getTimeWindowMinutes() {
        return timeWindowMinutes;
    }

    public void setTimeWindowMinutes(int timeWindowMinutes) {
        this.timeWindowMinutes = timeWindowMinutes;
    }

    public int getLateAfterMinutes() {
        return lateAfterMinutes;
    }

    public void setLateAfterMinutes(int lateAfterMinutes) {
        this.lateAfterMinutes = lateAfterMinutes;
    }

    public int getQrRotateSeconds() {
        return qrRotateSeconds;
    }

    public void setQrRotateSeconds(int qrRotateSeconds) {
        this.qrRotateSeconds = qrRotateSeconds;
    }

    public String getSessionSecret() {
        return sessionSecret;
    }

    public void setSessionSecret(String sessionSecret) {
        this.sessionSecret = sessionSecret;
    }

    public Instant getCheckinOpenAt() {
        return checkinOpenAt;
    }

    public void setCheckinOpenAt(Instant checkinOpenAt) {
        this.checkinOpenAt = checkinOpenAt;
    }

    public Instant getCheckinCloseAt() {
        return checkinCloseAt;
    }

    public void setCheckinCloseAt(Instant checkinCloseAt) {
        this.checkinCloseAt = checkinCloseAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}