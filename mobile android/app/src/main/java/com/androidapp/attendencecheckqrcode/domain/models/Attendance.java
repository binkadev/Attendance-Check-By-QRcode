package com.androidapp.attendencecheckqrcode.domain.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Attendance implements Serializable {

    @SerializedName("classId")
    private String classId;

    @SerializedName("studentId")
    private String studentId;

    @SerializedName("studentName")
    private String studentName;

    @SerializedName("date")
    private String date;

    @SerializedName("time")
    private String time;

    public Attendance() {}

    public Attendance(String classId, String studentId, String studentName, String date, String time) {
        this.classId = classId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.date = date;
        this.time = time;
    }

    public String toFileString() {
        return classId + "|" + studentId + "|" + studentName + "|" + date + "|" + time;
    }

    public String getClassId() { return classId; }
    public String getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getDate() { return date; }
    public String getTime() { return time; }
}