package com.ptithcm.attendapp.model;

import com.google.gson.annotations.SerializedName;

public class CheckInQrResponse {
    @SerializedName("sessionId")
    private String sessionId;

    @SerializedName("userId")
    private String userId;

    @SerializedName("attendanceStatus")
    private String attendanceStatus;

    @SerializedName("checkInAt")
    private String checkInAt;

    @SerializedName("qrTokenId")
    private String qrTokenId;

    // Getters
    public String getSessionId() { return sessionId; }
    public String getUserId() { return userId; }
    public String getAttendanceStatus() { return attendanceStatus; }
    public String getCheckInAt() { return checkInAt; }
    public String getQrTokenId() { return qrTokenId; }
}