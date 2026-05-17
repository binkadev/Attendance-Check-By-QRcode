package com.ptithcm.attendapp.model;
import com.google.gson.annotations.SerializedName;

public class UpcomingSessionItem {
    @SerializedName("attendanceSessionId") private String attendanceSessionId;
    @SerializedName("attendanceStatus") private String attendanceStatus;

    // THÊM 5 TRƯỜNG THỜI GIAN MỚI
    @SerializedName("checkinOpenAt") private String checkinOpenAt;
    @SerializedName("checkinCloseAt") private String checkinCloseAt;
    @SerializedName("sessionDate") private String sessionDate;
    @SerializedName("startTime") private String startTime;
    @SerializedName("endTime") private String endTime;

    @SerializedName("groupId") private String groupId;
    @SerializedName("sessionName") private String sessionName;
    @SerializedName("startAt") private String startAt;
    @SerializedName("endAt") private String endAt;
    @SerializedName("room") private String room;
    @SerializedName("groupName") private String groupName;

    // THÊM TRƯỜNG GIẢNG VIÊN (Sửa lỗi báo đỏ)
    @SerializedName("lecturerName") private String lecturerName;

    // --- GETTERS ---
    public String getAttendanceSessionId() { return attendanceSessionId; }
    public String getAttendanceStatus() { return attendanceStatus; }

    public String getCheckinOpenAt() { return checkinOpenAt; }
    public String getCheckinCloseAt() { return checkinCloseAt; }
    public String getSessionDate() { return sessionDate; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }

    public String getGroupId() { return groupId; }
    public String getSessionName() { return sessionName; }
    public String getStartAt() { return startAt; }
    public String getEndAt() { return endAt; }
    public String getRoom() { return room; }
    public String getGroupName() { return groupName; }

    public String getLecturerName() { return lecturerName; }
}