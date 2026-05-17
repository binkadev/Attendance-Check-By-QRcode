package com.ptithcm.attendapp.model;

import java.util.List;

public class GroupDetail {
    private String id;
    private String name;
    private String courseCode;
    private String classCode;
    private String semester;
    private String academicYear;
    private String room;
    private String lecturerName;
    private List<WeeklySchedule> weeklySchedules;

    // Lớp nội bộ để chứa lịch học trong tuần
    public static class WeeklySchedule {
        private String dayOfWeek;
        private String startTime;
        private String endTime;

        public String getDayOfWeek() { return dayOfWeek; }
        public String getStartTime() { return startTime; }
        public String getEndTime() { return endTime; }
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getCourseCode() { return courseCode; }
    public String getClassCode() { return classCode; }
    public String getSemester() { return semester; }
    public String getAcademicYear() { return academicYear; }
    public String getRoom() { return room; }
    public String getLecturerName() {return lecturerName;}
    public List<WeeklySchedule> getWeeklySchedules() { return weeklySchedules; }
}