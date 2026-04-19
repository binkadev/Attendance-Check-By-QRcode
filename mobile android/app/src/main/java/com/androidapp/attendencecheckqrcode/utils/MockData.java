package com.androidapp.attendencecheckqrcode.utils;

import android.content.Context;
import com.androidapp.attendencecheckqrcode.domain.models.Attendance;
import com.androidapp.attendencecheckqrcode.domain.models.User;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MockData {
    private static final String FILE_USERS = "users.txt";
    private static final String FILE_CLASSES = "classes.txt";
    private static final String FILE_ATTENDANCE = "attendance.txt";
    private static final String FILE_ENROLLMENTS = "enrollments.txt"; // Lưu: user_id|class_id

    // --- 1. LOGIC QUÉT QR & ĐIỂM DANH ---
    public static void processQRCode(Context context, User student, String qrContent) {
        // QR Format giả định: "ClassID_TimeStamp" (VD: "UUID-1234_173456789")
        String[] parts = qrContent.split("_");
        if (parts.length < 2) return;

        String classId = parts[0];

        // A. Tự động tham gia lớp nếu chưa tham gia
        if (!isUserEnrolled(context, student.getId(), classId)) {
            joinClass(context, student.getId(), classId);
        }

        // B. Lưu điểm danh
        java.text.SimpleDateFormat sdfDate = new java.text.SimpleDateFormat("dd/MM/yyyy");
        java.text.SimpleDateFormat sdfTime = new java.text.SimpleDateFormat("HH:mm:ss");
        java.util.Date now = new java.util.Date();

        Attendance att = new Attendance(
                classId,
                student.getId(),
                student.getFullName(),
                sdfDate.format(now),
                sdfTime.format(now)
        );

        saveAttendanceToFile(context, att);
    }


    // Lưu thông tin user tham gia lớp vào file enrollments.txt
    private static void joinClass(Context context, int userId, String classId) {
        String data = userId + "|" + classId;
        appendToFile(context, FILE_ENROLLMENTS, data);
    }

    // Kiểm tra user đã có trong file enrollments.txt chưa
    private static boolean isUserEnrolled(Context context, int userId, String classId) {
        List<String> joinedClasses = getJoinedClassIds(context, userId);
        return joinedClasses.contains(classId);
    }

    // Đọc file lấy danh sách ID các lớp mà user đã tham gia
    private static List<String> getJoinedClassIds(Context context, int userId) {
        List<String> ids = new ArrayList<>();
        try (FileInputStream fis = context.openFileInput(FILE_ENROLLMENTS);
             BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split("\\|");
                // Format: userId|classId
                if (p.length >= 2 && Integer.parseInt(p[0]) == userId) {
                    ids.add(p[1]);
                }
            }
        } catch (Exception e) {}
        return ids;
    }

    private static void saveAttendanceToFile(Context context, Attendance att) {
        appendToFile(context, FILE_ATTENDANCE, att.toFileString());
    }

    // --- 2. USER ---
    public static void saveUserToTextFile(Context context, User user) {
        appendToFile(context, FILE_USERS, user.toFileString());
    }

    public static User checkLogin(Context context, String email, String password) {
        try (FileInputStream fis = context.openFileInput(FILE_USERS);
             BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {
            String line;
            while ((line = br.readLine()) != null) {
                User u = new User(line);
                if (u.getEmail().equalsIgnoreCase(email) && u.getPassword().equals(password)) return u;
            }
        } catch (Exception e) {}

        // Admin fallback
        if (email.equals("admin") && password.equals("1"))
            return new User("admin", "1", "Admin", "User");

        return null;
    }

    public static List<User> getAllUsers(Context context) {
        List<User> list = new ArrayList<>();
        try (FileInputStream fis = context.openFileInput(FILE_USERS);
             BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) list.add(new User(line));
            }
        } catch (Exception e) {}
        return list;
    }

    // --- 3. CLASSROOM ---
    public static void saveClassToFile(Context context, Attendance.Classroom classroom) {
        appendToFile(context, FILE_CLASSES, classroom.toFileString());
    }

    // Lấy danh sách lớp DẠY (Giảng viên)
    public static List<Attendance.Classroom> getTeachingClasses(Context context, int lecturerId) {
        List<Attendance.Classroom> all = getAllClasses(context);
        List<Attendance.Classroom> result = new ArrayList<>();
        for (Attendance.Classroom c : all) {
            if (c.getLecturerId() == lecturerId) result.add(c);
        }
        return result;
    }

    // Lấy danh sách lớp HỌC (Sinh viên)
    // CẬP NHẬT LOGIC: Chỉ lấy lớp đã tham gia (có trong enrollments.txt) HOẶC lớp không phải mình tạo
    public static List<Attendance.Classroom> getEnrolledClasses(Context context, int userId) {
        List<Attendance.Classroom> all = getAllClasses(context);
        List<String> joinedIds = getJoinedClassIds(context, userId); // Lấy list ID lớp đã join

        List<Attendance.Classroom> result = new ArrayList<>();
        for (Attendance.Classroom c : all) {
            // Điều kiện: (Đã Join OR Không phải mình dạy) AND (Tránh trùng lặp logic)
            // Logic đơn giản cho demo: Lớp nào có trong file enrollment của mình thì hiện ra
            if (joinedIds.contains(c.getClassId())) {
                result.add(c);
            }
            // Fallback demo: Nếu chưa join lớp nào, hiện các lớp mình không dạy để test
            else if (joinedIds.isEmpty() && c.getLecturerId() != userId) {
                result.add(c);
            }
        }
        return result;
    }

    private static List<Attendance.Classroom> getAllClasses(Context context) {
        List<Attendance.Classroom> list = new ArrayList<>();
        try (FileInputStream fis = context.openFileInput(FILE_CLASSES);
             BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) list.add(new Attendance.Classroom(line));
            }
        } catch (Exception e) {}
        return list;
    }

    // --- HELPER ---
    private static void appendToFile(Context context, String fileName, String data) {
        try (FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_APPEND)) {
            fos.write((data + "\n").getBytes());
        } catch (Exception e) { e.printStackTrace(); }
    }
}