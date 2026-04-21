package com.attendance.backend.group.dto;

import com.attendance.backend.domain.enums.ApprovalMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

public class UpdateGroupRequest {

    @Size(min = 3, max = 150, message = "name length must be between 3 and 150")
    private String name;

    @Size(max = 50, message = "courseCode length must be <= 50")
    private String courseCode;

    @Size(max = 50, message = "classCode length must be <= 50")
    private String classCode;

    @Size(min = 6, max = 16, message = "joinCode length must be between 6 and 16")
    private String joinCode;

    @Size(max = 1000, message = "description length must be <= 1000")
    private String description;

    @Size(max = 30, message = "semester length must be <= 30")
    private String semester;

    @Size(max = 30, message = "academicYear length must be <= 30")
    private String academicYear;

    @Size(max = 120, message = "campus length must be <= 120")
    private String campus;

    @Size(max = 80, message = "room length must be <= 80")
    private String room;

    private ApprovalMode approvalMode;

    private Boolean allowAutoJoinOnCheckin;

    @Valid
    @Size(min = 1, message = "weeklySchedules must not be empty")
    private List<GroupWeeklyScheduleRequest> weeklySchedules;

    @Min(value = 1, message = "totalSessions must be greater than 0")
    private Integer totalSessions;

    @Min(value = 0, message = "maxAllowedAbsences must be greater than or equal to 0")
    private Integer maxAllowedAbsences;

    public UpdateGroupRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getClassCode() {
        return classCode;
    }

    public void setClassCode(String classCode) {
        this.classCode = classCode;
    }

    public String getJoinCode() {
        return joinCode;
    }

    public void setJoinCode(String joinCode) {
        this.joinCode = joinCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
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

    public ApprovalMode getApprovalMode() {
        return approvalMode;
    }

    public void setApprovalMode(ApprovalMode approvalMode) {
        this.approvalMode = approvalMode;
    }

    public Boolean getAllowAutoJoinOnCheckin() {
        return allowAutoJoinOnCheckin;
    }

    public void setAllowAutoJoinOnCheckin(Boolean allowAutoJoinOnCheckin) {
        this.allowAutoJoinOnCheckin = allowAutoJoinOnCheckin;
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

    public Integer getMaxAllowedAbsences() {
        return maxAllowedAbsences;
    }

    public void setMaxAllowedAbsences(Integer maxAllowedAbsences) {
        this.maxAllowedAbsences = maxAllowedAbsences;
    }
}