package com.ptithcm.attendapp.model;
import com.google.gson.annotations.SerializedName;

public class AttendanceHistoryItem {
    @SerializedName("sessionId")
    private String sessionId;
    @SerializedName("sessionName")
    private String sessionName; // VD: "Buổi 1", "Buổi 2"
    @SerializedName("sessionDate")
    private String sessionDate; // VD: "2026-05-04"
    @SerializedName("startTime")
    private String startTime;
    @SerializedName("endTime")
    private String endTime;
    @SerializedName("attendanceStatus")
    private String attendanceStatus; // "PRESENT", "ABSENT", "LATE"
    @SerializedName("checkInAt")
    private String checkInAt;
    @SerializedName("suspiciousFlag")
    private boolean suspiciousFlag;

    // Các Getters cần thiết
    public String getSessionName() { return sessionName; }
    public String getSessionDate() { return sessionDate; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getAttendanceStatus() { return attendanceStatus; }
    public String getCheckInAt() { return checkInAt; }
    public boolean isSuspiciousFlag() { return suspiciousFlag; }
}