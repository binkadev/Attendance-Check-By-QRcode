package com.androidapp.attendencecheckqrcode.domain.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Attendance implements Serializable {

    // --- LỚP ĐIỂM DANH (CÁ NHÂN) ---
    @SerializedName("class_id")
    private String classId;
    @SerializedName("student_id")
    private int studentId;
    @SerializedName("student_name")
    private String studentName;
    @SerializedName("date")
    private String date;
    @SerializedName("time")
    private String time;

    public Attendance(String classId, int studentId, String studentName, String date, String time) {
        this.classId = classId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.date = date;
        this.time = time;
    }

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

    public String getDate() { return date; }
    public String getTime() { return time; }

    // --- LỚP HỌC (CLASSROOM) ---
    public static class Classroom implements Serializable {

        // 1. Map 'id' của Backend vào 'classId' của Android
        @SerializedName(value = "id", alternate = {"class_id", "classId"})
        private String classId;

        // 2. Map 'name' của Backend vào 'className'
        @SerializedName(value = "name", alternate = {"class_name", "className"})
        private String className;

        // 3. Map 'joinCode' (Mã tự sinh) vào 'subjectCode' để lát nữa hiện ra UI cho giảng viên thấy
        @SerializedName(value = "joinCode", alternate = {"subject_code", "subjectCode"})
        private String subjectCode;

        // 4. Map 'code' (Mã môn-Mã lớp) vào 'classCode'
        @SerializedName(value = "code", alternate = {"class_code", "classCode"})
        private String classCode;

        @SerializedName("room")
        private String room;

        @SerializedName("day_of_week")
        private String dayOfWeek;

        @SerializedName("time_slot")
        private String timeSlot;

        @SerializedName("total_sessions")
        private int totalSessions;

        @SerializedName("max_absences")
        private int maxAbsences;

        @SerializedName("semester")
        private String semester;

        @SerializedName("description")
        private String description;

        @SerializedName("lecturer_id")
        private int lecturerId;

        @SerializedName("lecturer_name")
        private String lecturerName;

        @SerializedName("total_students")
        private int totalStudents;

        public Classroom(String classId, String className, String subjectCode, String classCode, String room,
                         String dayOfWeek, String timeSlot, int totalSessions,
                         int maxAbsences, String semester, String description,
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
            this.totalStudents = 0;
        }

        public Classroom(String line) {
            String[] p = line.split("\\|");
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

        public String toFileString() {
            return classId + "|" + className + "|" + subjectCode + "|" + classCode + "|" +
                    room + "|" + dayOfWeek + "|" + timeSlot + "|" + totalSessions + "|" +
                    maxAbsences + "|" + semester + "|" + description + "|" +
                    lecturerId + "|" + lecturerName + "|" + totalStudents;
        }

        public String getClassId() { return classId; }
        public String getClassName() { return className; }

        // Getter này giờ sẽ trả về mã JoinCode (Ví dụ: INT_A7B2_48291)
        public String getSubjectCode() { return subjectCode; }

        // Getter này trả về mã môn-lớp (Ví dụ: INT1340-D22CQAT01)
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