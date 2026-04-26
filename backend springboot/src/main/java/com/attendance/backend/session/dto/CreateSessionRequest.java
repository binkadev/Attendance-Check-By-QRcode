package com.attendance.backend.session.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class CreateSessionRequest {

    @Size(max = 150, message = "title must be at most 150 characters")
    private String title;

    @NotNull(message = "startAt is required")
    private Instant startAt;

    private Instant endAt;

    @Min(value = 1, message = "timeWindowMinutes must be at least 1")
    @Max(value = 300, message = "timeWindowMinutes must be at most 300")
    private Integer timeWindowMinutes;

    @Min(value = 1, message = "lateAfterMinutes must be at least 1")
    @Max(value = 120, message = "lateAfterMinutes must be at most 120")
    private Integer lateAfterMinutes;

    @Min(value = 5, message = "qrRotateSeconds must be at least 5")
    @Max(value = 120, message = "qrRotateSeconds must be at most 120")
    private Integer qrRotateSeconds;

    private Instant checkinOpenAt;

    private Instant checkinCloseAt;

    private Boolean allowManualOverride;

    @Size(max = 500, message = "note must be at most 500 characters")
    private String note;

    public CreateSessionRequest() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public Integer getTimeWindowMinutes() {
        return timeWindowMinutes;
    }

    public void setTimeWindowMinutes(Integer timeWindowMinutes) {
        this.timeWindowMinutes = timeWindowMinutes;
    }

    public Integer getLateAfterMinutes() {
        return lateAfterMinutes;
    }

    public void setLateAfterMinutes(Integer lateAfterMinutes) {
        this.lateAfterMinutes = lateAfterMinutes;
    }

    public Integer getQrRotateSeconds() {
        return qrRotateSeconds;
    }

    public void setQrRotateSeconds(Integer qrRotateSeconds) {
        this.qrRotateSeconds = qrRotateSeconds;
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

    public Boolean getAllowManualOverride() {
        return allowManualOverride;
    }

    public void setAllowManualOverride(Boolean allowManualOverride) {
        this.allowManualOverride = allowManualOverride;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}