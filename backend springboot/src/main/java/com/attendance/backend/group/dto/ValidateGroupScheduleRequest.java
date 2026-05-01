package com.attendance.backend.group.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class ValidateGroupScheduleRequest {

    private UUID excludeGroupId;

    @Size(max = 120, message = "campus length must be <= 120")
    private String campus;

    @Size(max = 80, message = "room length must be <= 80")
    private String room;

    @NotNull(message = "startDate is required")
    private LocalDate startDate;

    @NotNull(message = "weeklySchedules is required")
    @Size(min = 1, message = "weeklySchedules must not be empty")
    @Valid
    private List<GroupWeeklyScheduleRequest> weeklySchedules;

    @NotNull(message = "totalSessions is required")
    @Min(value = 1, message = "totalSessions must be greater than 0")
    private Integer totalSessions;

    public UUID getExcludeGroupId() {
        return excludeGroupId;
    }

    public void setExcludeGroupId(UUID excludeGroupId) {
        this.excludeGroupId = excludeGroupId;
    }

    public String getCampus() {
        return campus;
    }

    public void setCampus(String campus) {
        this.campus = campus;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public List<GroupWeeklyScheduleRequest> getWeeklySchedules() {
        return weeklySchedules;
    }

    public void setWeeklySchedules(List<GroupWeeklyScheduleRequest> weeklySchedules) {
        this.weeklySchedules = weeklySchedules;
    }

    public Integer getTotalSessions() {
        return totalSessions;
    }

    public void setTotalSessions(Integer totalSessions) {
        this.totalSessions = totalSessions;
    }
}
