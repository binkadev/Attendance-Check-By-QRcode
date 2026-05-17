package com.ptithcm.attendapp.model;
import java.io.Serializable;

public class ClassItem implements Serializable{
    private String groupId;
    private String groupName;
    private String courseCode;
    private String classCode;
    private String startTime;
    private String endTime;
    private String room;
    private String lecturerName;
    private String myMemberStatus; // Trạng thái tham gia
    private String myRole;

    // Getters
    public String getGroupId() { return groupId; }
    public String getGroupName() { return groupName; }
    public String getCourseCode() { return courseCode; }
    public String getClassCode() { return classCode; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getRoom() { return room; }
    public String getLecturerName() { return lecturerName; }
    public String getMyMemberStatus() { return myMemberStatus; }
    public String getMyRole() { return myRole; }
}