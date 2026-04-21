//package com.androidapp.attendencecheckqrcode.utils;
//
//import android.content.Context;
//import com.androidapp.attendencecheckqrcode.domain.models.Attendance;
//import com.androidapp.attendencecheckqrcode.domain.models.User;
//import java.io.BufferedReader;
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.io.InputStreamReader;
//import java.util.ArrayList;
//import java.util.List;
//
//public class MockData {
//    private static final String FILE_USERS = "users.txt";
//    private static final String FILE_CLASSES = "classes.txt";
//    private static final String FILE_ATTENDANCE = "attendance.txt";
//    private static final String FILE_ENROLLMENTS = "enrollments.txt"; // Lưu: user_id|class_id
//
//    // --- 1. LOGIC QUÉT QR & ĐIỂM DANH ---
//    public static void processQRCode(Context context, User student, String qrContent) {
//        // QR Format giả định: "ClassID_TimeStamp" (VD: "UUID-1234_173456789")
//        String[] parts = qrContent.split("_");
//        if (parts.length < 2) return;
//
//        String classId = parts[0];
//
//        // A. Tự động tham gia lớp nếu chưa tham gia (student.getId() giờ là String)
//        if (!isUserEnrolled(context, student.getId(), classId)) {
//            joinClass(context, student.getId(), classId);
//        }
//
//        // B. Lưu điểm danh
//        java.text.SimpleDateFormat sdfDate = new java.text.SimpleDateFormat("dd/MM/yyyy");
//        java.text.SimpleDateFormat sdfTime = new java.text.SimpleDateFormat("HH:mm:ss");
//        java.util.Date now = new java.util.Date();
//
//        Attendance att = new Attendance(
//                classId,
//                student.getId(), // Đã là String
//                student.getFullName(),
//                sdfDate.format(now),
//                sdfTime.format(now)
//        );
//
//        saveAttendanceToFile(context, att);
//    }
//
//    // ĐÃ SỬA: int userId -> String userId
//    private static void joinClass(Context context, String userId, String classId) {
//        String data = userId + "|" + classId;
//        appendToFile(context, FILE_ENROLLMENTS, data);
//    }
//
//    // ĐÃ SỬA: int userId -> String userId
//    private static boolean isUserEnrolled(Context context, String userId, String classId) {
//        List<String> joinedClasses = getJoinedClassIds(context, userId);
//        return joinedClasses.contains(classId);
//    }
//
//    // ĐÃ SỬA: int userId -> String userId
//    private static List<String> getJoinedClassIds(Context context, String userId) {
//        List<String> ids = new ArrayList<>();
//        try (FileInputStream fis = context.openFileInput(FILE_ENROLLMENTS);
//             BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {
//            String line;
//            while ((line = br.readLine()) != null) {
//                String[] p = line.split("\\|");
//                // ĐÃ SỬA: Dùng .equals() thay vì Integer.parseInt
//                if (p.length >= 2 && p[0].equals(userId)) {
//                    ids.add(p[1]);
//                }
//            }
//        } catch (Exception e) {}
//        return ids;
//    }
//
//    private static void saveAttendanceToFile(Context context, Attendance att) {
//        appendToFile(context, FILE_ATTENDANCE, att.toFileString());
//    }
//
//    // --- 2. USER ---
//    public static void saveUserToTextFile(Context context, User user) {
//        // Hàm toFileString() có thể đã bị xóa ở User.java, nếu bạn không dùng MockData tạo user nữa thì có thể comment lại
//        // appendToFile(context, FILE_USERS, user.toFileString());
//    }
//
//    public static User checkLogin(Context context, String email, String password) {
//        // Tạm ẩn đọc file cũ vì class User đã thay đổi
//        /*
//        try (FileInputStream fis = context.openFileInput(FILE_USERS);
//             BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {
//            String line;
//            while ((line = br.readLine()) != null) {
//                User u = new User(line);
//                if (u.getEmail().equalsIgnoreCase(email) && u.getPassword().equals(password)) return u;
//            }
//        } catch (Exception e) {}
//        */
//
//        // Admin fallback (Cập nhật Constructor cho khớp bản User mới: id, email, fullName, userCode)
//        if (email.equals("admin") && password.equals("1"))
//            return new User("admin-id-123", "admin", "Admin System", "ADMIN001");
//
//        return null;
//    }
//
//    public static List<User> getAllUsers(Context context) {
//        List<User> list = new ArrayList<>();
//        // Tạm ẩn vì không dùng đọc file text cho User nữa
//        return list;
//    }
//
//    // --- 3. CLASSROOM ---
//    public static void saveClassToFile(Context context, Attendance.Classroom classroom) {
//        appendToFile(context, FILE_CLASSES, classroom.toFileString());
//    }
//
//    // ĐÃ SỬA: int lecturerId -> String lecturerId
//    public static List<Attendance.Classroom> getTeachingClasses(Context context, String lecturerId) {
//        List<Attendance.Classroom> all = getAllClasses(context);
//        List<Attendance.Classroom> result = new ArrayList<>();
//        for (Attendance.Classroom c : all) {
//            // Dùng String.valueOf để phòng trường hợp Classroom cũ vẫn dùng int
//            if (String.valueOf(c.getLecturerId()).equals(lecturerId)) result.add(c);
//        }
//        return result;
//    }
//
//    // ĐÃ SỬA: int userId -> String userId
//    public static List<Attendance.Classroom> getEnrolledClasses(Context context, String userId) {
//        List<Attendance.Classroom> all = getAllClasses(context);
//        List<String> joinedIds = getJoinedClassIds(context, userId);
//
//        List<Attendance.Classroom> result = new ArrayList<>();
//        for (Attendance.Classroom c : all) {
//            if (joinedIds.contains(c.getClassId())) {
//                result.add(c);
//            }
//            else if (joinedIds.isEmpty() && !String.valueOf(c.getLecturerId()).equals(userId)) {
//                result.add(c);
//            }
//        }
//        return result;
//    }
//
//    private static List<Attendance.Classroom> getAllClasses(Context context) {
//        List<Attendance.Classroom> list = new ArrayList<>();
//        try (FileInputStream fis = context.openFileInput(FILE_CLASSES);
//             BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {
//            String line;
//            while ((line = br.readLine()) != null) {
//                if (!line.trim().isEmpty()) list.add(new Attendance.Classroom(line));
//            }
//        } catch (Exception e) {}
//        return list;
//    }
//
//    // --- HELPER ---
//    private static void appendToFile(Context context, String fileName, String data) {
//        try (FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_APPEND)) {
//            fos.write((data + "\n").getBytes());
//        } catch (Exception e) { e.printStackTrace(); }
//    }
//}