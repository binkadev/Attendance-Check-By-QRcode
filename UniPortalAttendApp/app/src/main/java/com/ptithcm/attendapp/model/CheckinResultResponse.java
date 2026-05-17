package com.ptithcm.attendapp.model;

import com.google.gson.annotations.SerializedName;

public class CheckinResultResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("title")
    private String title;

    @SerializedName("message")
    private String message;

    @SerializedName("sessionId")
    private String sessionId;

    @SerializedName("groupId")
    private String groupId;

    @SerializedName("sessionName")
    private String sessionName;

    @SerializedName("subjectName")
    private String subjectName;

    @SerializedName("groupCode")
    private String groupCode;

    @SerializedName("courseCode")
    private String courseCode;

    @SerializedName("classCode")
    private String classCode;

    @SerializedName("displayCode")
    private String displayCode;

    @SerializedName("checkInAt")
    private String checkInAt;

    @SerializedName("attendanceStatus")
    private String attendanceStatus;

    @SerializedName("attendanceStatusLabel")
    private String attendanceStatusLabel;

    @SerializedName("checkInMethod")
    private String checkInMethod;

    @SerializedName("room")
    private String room;

    @SerializedName("campus")
    private String campus;

    @SerializedName("locationDisplay")
    private String locationDisplay;

    @SerializedName("locationSubtitle")
    private String locationSubtitle;

    // Getters
    public boolean isSuccess() { return success; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getSessionId() { return sessionId; }
    public String getGroupId() { return groupId; }
    public String getSessionName() { return sessionName; }
    public String getSubjectName() { return subjectName; }
    public String getGroupCode() { return groupCode; }
    public String getCourseCode() { return courseCode; }
    public String getClassCode() { return classCode; }
    public String getDisplayCode() { return displayCode; }
    public String getCheckInAt() { return checkInAt; }
    public String getAttendanceStatus() { return attendanceStatus; }
    public String getAttendanceStatusLabel() { return attendanceStatusLabel; }
    public String getCheckInMethod() { return checkInMethod; }
    public String getRoom() { return room; }
    public String getCampus() { return campus; }
    public String getLocationDisplay() { return locationDisplay; }
    public String getLocationSubtitle() { return locationSubtitle; }
}