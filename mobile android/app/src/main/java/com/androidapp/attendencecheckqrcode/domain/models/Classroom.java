package com.androidapp.attendencecheckqrcode.domain.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class Classroom implements Serializable {

    @SerializedName("groupId")
    private String groupId;

    @SerializedName("groupName")
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

    @SerializedName("locationDisplay")
    private String locationDisplay;

    @SerializedName("myRole")
    private String myRole; // "LECTURER" hoặc "STUDENT"

    @SerializedName("code")
    private String code;

    @SerializedName("weeklySchedules")
    private List<WeeklySchedule> weeklySchedules;

    public static class WeeklySchedule implements Serializable {
        @SerializedName("dayOfWeek")
        private String dayOfWeek;

        @SerializedName("startTime")
        private String startTime;

        @SerializedName("endTime")
        private String endTime;

        public String getDayOfWeek() { return dayOfWeek; }
        public String getStartTime() { return startTime; }
        public String getEndTime() { return endTime; }
    }

    public Classroom() {}

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

    public List<WeeklySchedule> getWeeklySchedules() { return weeklySchedules; }

    public String getDisplayTitle() {
        return classCode + " - " + groupName;
    }
}