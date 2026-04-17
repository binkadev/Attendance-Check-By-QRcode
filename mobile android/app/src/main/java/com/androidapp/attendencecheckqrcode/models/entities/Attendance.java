package com.androidapp.attendencecheckqrcode.models.entities;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Attendance implements Serializable {
    @SerializedName("class_id")
    private String classId;
    @SerializedName("student_id")
    private int studentId;
    @SerializedName("student_name")
    private String studentName;
    @SerializedName("date")
    private String date; // VD: 12/02/2026
    @SerializedName("time")
    private String time; // VD: 08:30:00

    public Attendance(String classId, int studentId, String studentName, String date, String time) {
        this.classId = classId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.date = date;
        this.time = time;
    }

    // Constructor đọc từ file
    public Attendance(String line) {
        String[] p = line.split("\\|");
        if (p.length >= 5) {
            this.classId = p[0];
            this.studentId = Integer.parseInt(p[1]);
            this.studentName = p[2];
            this.date = p[3];
            this.time = p[4];
        }
    }

    public String toFileString() {
        return classId + "|" + studentId + "|" + studentName + "|" + date + "|" + time;
    }

    // Getters...
    public String getDate() { return date; }
    public String getTime() { return time; }

    public static class Classroom implements Serializable {

        @SerializedName("class_id")
        private String classId;

        @SerializedName("class_name")
        private String className;

        @SerializedName("subject_code")
        private String subjectCode;

        @SerializedName("class_code")
        private String classCode;

        @SerializedName("room")
        private String room;

        @SerializedName("day_of_week")
        private String dayOfWeek;

        @SerializedName("time_slot")
        private String timeSlot;

        @SerializedName("total_sessions")
        private int totalSessions;

        // --- CÁC TRƯỜNG MỚI THÊM VÀO ---
        @SerializedName("max_absences")
        private int maxAbsences;

        @SerializedName("semester")
        private String semester;

        @SerializedName("description")
        private String description;
        // -------------------------------

        @SerializedName("lecturer_id")
        private int lecturerId;

        @SerializedName("lecturer_name")
        private String lecturerName;

        @SerializedName("total_students")
        private int totalStudents;

        // --- CONSTRUCTOR 1: Dùng cho CreateClassActivity (13 tham số) ---
        public Classroom(String classId, String className, String subjectCode, String classCode, String room,
                         String dayOfWeek, String timeSlot, int totalSessions,
                         int maxAbsences, String semester, String description, // Thêm 3 tham số này
                         int lecturerId, String lecturerName) {
            this.classId = classId;
            this.className = className;
            this.subjectCode = subjectCode;
            this.classCode = classCode;
            this.room = room;
            this.dayOfWeek = dayOfWeek;
            this.timeSlot = timeSlot;
            this.totalSessions = totalSessions;
            this.maxAbsences = maxAbsences;
            this.semester = semester;
            this.description = description;
            this.lecturerId = lecturerId;
            this.lecturerName = lecturerName;
            this.totalStudents = 0; // Mặc định 0 sinh viên
        }

        // --- CONSTRUCTOR 2: Đọc từ file txt (Cập nhật logic tách chuỗi) ---
        public Classroom(String line) {
            String[] p = line.split("\\|");
            // Bây giờ chuỗi có 14 phần tử (tính cả totalStudents)
            if (p.length >= 14) {
                this.classId = p[0];
                this.className = p[1];
                this.subjectCode = p[2];
                this.classCode = p[3];
                this.room = p[4];
                this.dayOfWeek = p[5];
                this.timeSlot = p[6];
                this.totalSessions = Integer.parseInt(p[7]);
                this.maxAbsences = Integer.parseInt(p[8]);
                this.semester = p[9];
                this.description = p[10];
                this.lecturerId = Integer.parseInt(p[11]);
                this.lecturerName = p[12];
                this.totalStudents = Integer.parseInt(p[13]);
            }
        }

        // --- HÀM GHI FILE (Cập nhật thêm trường mới vào chuỗi) ---
        public String toFileString() {
            return classId + "|" + className + "|" + subjectCode + "|" + classCode + "|" +
                    room + "|" + dayOfWeek + "|" + timeSlot + "|" + totalSessions + "|" +
                    maxAbsences + "|" + semester + "|" + description + "|" +
                    lecturerId + "|" + lecturerName + "|" + totalStudents;
        }

        // --- GETTERS ---
        public String getClassId() { return classId; }
        public String getClassName() { return className; }
        public String getSubjectCode() { return subjectCode; }
        public String getClassCode() { return classCode; }
        public String getRoom() { return room; }
        public String getDayOfWeek() { return dayOfWeek; }
        public String getTimeSlot() { return timeSlot; }
        public int getTotalSessions() { return totalSessions; }
        public int getMaxAbsences() { return maxAbsences; }
        public String getSemester() { return semester; }
        public String getDescription() { return description; }
        public int getLecturerId() { return lecturerId; }
        public String getLecturerName() { return lecturerName; }
        public int getTotalStudents() { return totalStudents; }
    }
}
