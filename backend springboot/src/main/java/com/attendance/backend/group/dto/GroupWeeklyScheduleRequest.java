package com.attendance.backend.group.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class GroupWeeklyScheduleRequest {

    @NotBlank(message = "dayOfWeek is required")
    private String dayOfWeek;

    @NotBlank(message = "startTime is required")
    @Pattern(
            regexp = "^([01]\\d|2[0-3]):[0-5]\\d$",
            message = "startTime must be in HH:mm format"
    )
    private String startTime;

    @NotBlank(message = "endTime is required")
    @Pattern(
            regexp = "^([01]\\d|2[0-3]):[0-5]\\d$",
            message = "endTime must be in HH:mm format"
    )
    private String endTime;

    public GroupWeeklyScheduleRequest() {
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
}