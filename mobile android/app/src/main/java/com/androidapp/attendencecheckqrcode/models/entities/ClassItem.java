package com.androidapp.attendencecheckqrcode.models.entities;

//package com.androidapp.attendencecheckqrcode.models;

public class ClassItem {
    private String className;
    private String subjectCode; // Mã môn
    private String classCode;   // Mã lớp
    private String lecturer;    // Giảng viên
    private String time;
    private String room;
    private int studentCount;

    public ClassItem(String className, String subjectCode, String classCode, String lecturer, String time, String room, int studentCount) {
        this.className = className;
        this.subjectCode = subjectCode;
        this.classCode = classCode;
        this.lecturer = lecturer;
        this.time = time;
        this.room = room;
        this.studentCount = studentCount;
    }

    // Getter methods
    public String getClassName() { return className; }
    public String getSubjectCode() { return subjectCode; }
    public String getClassCode() { return classCode; }
    public String getLecturer() { return lecturer; }
    public String getTime() { return time; }
    public String getRoom() { return room; }
    public int getStudentCount() { return studentCount; }
}
