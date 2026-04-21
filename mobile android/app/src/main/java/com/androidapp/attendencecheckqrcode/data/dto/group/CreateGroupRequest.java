package com.androidapp.attendencecheckqrcode.data.dto.group;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CreateGroupRequest {
    @SerializedName("name")
    private String name;
    @SerializedName("code")
    private String code;
    @SerializedName("courseCode")
    private String courseCode;
    @SerializedName("classCode")
    private String classCode;
    @SerializedName("joinCode")
    private String joinCode;
    @SerializedName("description")
    private String description;
    @SerializedName("semester")
    private String semester;
    @SerializedName("academicYear")
    private String academicYear;
    @SerializedName("campus")
    private String campus;
    @SerializedName("room")
    private String room;
    @SerializedName("approvalMode")
    private String approvalMode;
    @SerializedName("allowAutoJoinOnCheckin")
    private boolean allowAutoJoinOnCheckin;
    @SerializedName("weeklySchedules")
    private List<WeeklySchedule> weeklySchedules;
    @SerializedName("totalSessions")
    private int totalSessions;
    @SerializedName("maxAllowedAbsences")
    private int maxAllowedAbsences;

    public static class WeeklySchedule {
        @SerializedName("dayOfWeek")
        private String dayOfWeek;
        @SerializedName("startTime")
        private String startTime;
        @SerializedName("endTime")
        private String endTime;

        public WeeklySchedule(String dayOfWeek, String startTime, String endTime) {
            this.dayOfWeek = dayOfWeek;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    public CreateGroupRequest(String name, String code, String courseCode, String classCode,
                              String joinCode, String description, String semester,
                              String academicYear, String campus, String room, String approvalMode,
                              boolean allowAutoJoinOnCheckin, List<WeeklySchedule> weeklySchedules,
                              int totalSessions, int maxAllowedAbsences) {
        this.name = name;
        this.code = code;
        this.courseCode = courseCode;
        this.classCode = classCode;
        this.joinCode = joinCode;
        this.description = description;
        this.semester = semester;
        this.academicYear = academicYear;
        this.campus = campus;
        this.room = room;
        this.approvalMode = approvalMode;
        this.allowAutoJoinOnCheckin = allowAutoJoinOnCheckin;
        this.weeklySchedules = weeklySchedules;
        this.totalSessions = totalSessions;
        this.maxAllowedAbsences = maxAllowedAbsences;
    }
}