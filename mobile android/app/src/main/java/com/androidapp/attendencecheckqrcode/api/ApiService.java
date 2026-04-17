package com.androidapp.attendencecheckqrcode.api;

import com.androidapp.attendencecheckqrcode.models.entities.Attendance;
import com.androidapp.attendencecheckqrcode.models.payloads.AuthResponse;
import com.androidapp.attendencecheckqrcode.models.payloads.LoginRequest;
import com.androidapp.attendencecheckqrcode.models.payloads.RegisterRequest;
import com.androidapp.attendencecheckqrcode.models.entities.User;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // --- 1. AUTH & USER (auth-controller & me-controller) ---
    @POST("api/v1/auth/login")
    Call<AuthResponse> loginUser(@Body LoginRequest request);

    @POST("api/v1/auth/register")
    Call<AuthResponse> registerUser(@Body RegisterRequest request);

    @GET("api/v1/me")
    Call<User> getMyProfile();

    // API Quên mật khẩu (Giả định endpoint, điều chỉnh lại theo backend)
    @POST("api/v1/auth/forgot-password")
    Call<Object> forgotPassword(@Query("email") String email); // Thay Object bằng ApiResponse của bạn

    // --- 2. QR & ATTENDANCE (qr-token, attendance-admin, attendance-qr) ---
    
    // Đổi/Tạo mới QR động (Chạy định kỳ 10-15s)
    @POST("api/v1/sessions/{sessionId}/qr/rotate")
    Call<Void> rotateQrCode(@Path("sessionId") int sessionId); 

    // Sinh viên quét QR để điểm danh
    @POST("api/v1/sessions/{sessionId}/checkin/qr")
    Call<Void> checkinQr(@Path("sessionId") int sessionId /* Cần thêm @Body chứa chuỗi QR sinh viên quét được */);

    // Mở lại phiên điểm danh
    @POST("api/v1/sessions/{sessionId}/checkin/reopen")
    Call<Void> reopenCheckin(@Path("sessionId") int sessionId);

    // Reset điểm danh của 1 sinh viên (Sửa thủ công)
    @POST("api/v1/sessions/{sessionId}/attendance/{userId}/reset")
    Call<Void> resetAttendance(@Path("sessionId") int sessionId, @Path("userId") int userId);

    // Lấy danh sách sự kiện điểm danh (Danh sách sinh viên đã quét)
    @GET("api/v1/sessions/{sessionId}/attendance-events")
    Call<List<Object>> getAttendanceEvents(@Path("sessionId") int sessionId); // Thay Object bằng Model tương ứng

    // --- 3. CLASS & TEACHING (Thêm mới) ---

    // Sinh viên: Lấy danh sách lớp đang tham gia
    @GET("api/v1/classes/enrolled")
    Call<List<Attendance.Classroom>> getEnrolledClasses();

    // Giảng viên: Lấy danh sách lớp đang giảng dạy
    @GET("api/v1/classes/teaching")
    Call<List<Attendance.Classroom>> getTeachingClasses();

    // Thêm vào phần CLASS & TEACHING
    @POST("api/v1/classes")
    Call<Attendance.Classroom> createClass(@Body Attendance.Classroom newClass);

}