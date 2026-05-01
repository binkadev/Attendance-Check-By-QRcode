package com.androidapp.attendencecheckqrcode.domain.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class Classroom implements Serializable {

    // CHÚ Ý: alternate giúp Gson bắt được cả "groupId" VÀ "id"
    @SerializedName(value = "groupId", alternate = {"id"})
    private String groupId;

    @SerializedName(value = "groupName", alternate = {"name"})
    private String groupName;

    @SerializedName("courseCode")
    private String courseCode;

    @SerializedName("classCode")
    private String classCode;

    @SerializedName("room")
    private String room;

    @SerializedName("lecturerName")
    private String lecturerName;

    @SerializedName("approvedStudentCount")
    private int approvedStudentCount;

    @SerializedName("semester")
    private String semester;

    @SerializedName("academicYear")
    private String academicYear;

    @SerializedName(value = "locationDisplay", alternate = {"campus"})
    private String locationDisplay;

    @SerializedName("myRole")
    private String myRole;

    @SerializedName("code")
    private String code;

    @SerializedName("startTime")
    private String startTime;

    @SerializedName("endTime")
    private String endTime;

    @SerializedName("joinCode")
    private String joinCode;

    @SerializedName("description")
    private String description;

    @SerializedName("totalSessions")
    private int totalSessions;

    @SerializedName("maxAllowedAbsences")
    private int maxAllowedAbsences;

    @SerializedName("weeklySchedules")
    private List<WeeklySchedule> weeklySchedules;

    public Classroom() {}

    public static class WeeklySchedule implements Serializable {
        @SerializedName("dayOfWeek")
        private String dayOfWeek;

        @SerializedName("startTime")
        private String startTime;

        @SerializedName("endTime")
        private String endTime;

        public String getDayOfWeek() { return dayOfWeek; }
        public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }

        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }

        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }
    }

    // --- GETTERS ---
    public String getCode() { return code; }
    public String getGroupId() { return groupId; }
    public String getGroupName() { return groupName; }
    public String getCourseCode() { return courseCode; }
    public String getClassCode() { return classCode; }
    public String getRoom() { return room; }
    public String getLecturerName() { return lecturerName; }
    public int getApprovedStudentCount() { return approvedStudentCount; }
    public String getSemester() { return semester; }
    public String getAcademicYear() { return academicYear; }
    public String getLocationDisplay() { return locationDisplay; }
    public String getMyRole() { return myRole; }

    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }

    public String getJoinCode() { return joinCode; }
    public String getDescription() { return description; }
    public int getTotalSessions() { return totalSessions; }
    public int getMaxAllowedAbsences() { return maxAllowedAbsences; }
    public List<WeeklySchedule> getWeeklySchedules() { return weeklySchedules; }

    // --- SETTERS ---
    public void setWeeklySchedules(List<WeeklySchedule> weeklySchedules) {
        this.weeklySchedules = weeklySchedules;
    }
    public void setJoinCode(String joinCode) {
        this.joinCode = joinCode;
    }
    public void setTotalSessions(int totalSessions) {
        this.totalSessions = totalSessions;
    }
    public void setMaxAllowedAbsences(int maxAllowedAbsences) {
        this.maxAllowedAbsences = maxAllowedAbsences;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}