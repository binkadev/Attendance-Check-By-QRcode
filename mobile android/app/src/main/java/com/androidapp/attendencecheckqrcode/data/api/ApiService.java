package com.androidapp.attendencecheckqrcode.data.api;

import com.androidapp.attendencecheckqrcode.data.dto.attendance.CheckinQrRequest;
import com.androidapp.attendencecheckqrcode.data.dto.group.CreateGroupRequest;
import com.androidapp.attendencecheckqrcode.data.dto.group.GroupResponse;
import com.androidapp.attendencecheckqrcode.data.dto.group.JoinGroupRequest;
import com.androidapp.attendencecheckqrcode.data.dto.group.MemberResponse;
import com.androidapp.attendencecheckqrcode.data.dto.teaching.AttendancePolicyRequest;
import com.androidapp.attendencecheckqrcode.data.dto.teaching.GroupStudentPolicyResponse;
import com.androidapp.attendencecheckqrcode.domain.models.Attendance;
import com.androidapp.attendencecheckqrcode.data.dto.auth.AuthResponse;
import com.androidapp.attendencecheckqrcode.data.dto.auth.LoginRequest;
import com.androidapp.attendencecheckqrcode.data.dto.auth.RegisterRequest;
import com.androidapp.attendencecheckqrcode.domain.models.User;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // --- 1. AUTH & USER ---
    @POST("api/v1/auth/login")
    Call<AuthResponse> loginUser(@Body LoginRequest request);

    @POST("api/v1/auth/register")
    Call<AuthResponse> registerUser(@Body RegisterRequest request);

    @GET("api/v1/me")
    Call<User> getMyProfile();

    @POST("api/v1/auth/forgot-password")
    Call<Object> forgotPassword(@Query("email") String email);


    // --- 2. GROUPS (CLASS) ---
    @POST("api/v1/groups")
    Call<GroupResponse> createClassGroup(@Body CreateGroupRequest request);

    // Dùng chung API lấy danh sách, Backend sẽ tự phân biệt bằng role (hoặc tự nhận diện qua Token)
    @GET("api/v1/groups")
    Call<List<Attendance.Classroom>> getEnrolledClasses(@Query("role") String role); // Gửi role="STUDENT"

    @GET("api/v1/groups")
    Call<List<Attendance.Classroom>> getTeachingClasses(@Query("role") String role); // Gửi role="LECTURER"

    @POST("api/v1/groups/join")
    Call<MemberResponse> joinClass(@Body JoinGroupRequest request);


    // --- 3. TEACHING & POLICY ---
    @PUT("api/v1/groups/{groupId}/attendance-policy")
    Call<Void> updateAttendancePolicy(@Path("groupId") String groupId, @Body AttendancePolicyRequest request);

    @GET("api/v1/groups/{groupId}/attendance-policy/students")
    Call<GroupStudentPolicyResponse> getTeachingClassDetails(@Path("groupId") String groupId);

    // --- 4. QR & ATTENDANCE  ---
    @POST("api/v1/sessions/{sessionId}/qr/rotate")
    Call<Void> rotateQrCode(@Path("sessionId") String sessionId);



    @POST("api/v1/sessions/{sessionId}/checkin/qr")
    Call<Void> checkinQr(
            @Path("sessionId") String sessionId,
            @Body CheckinQrRequest request
    );
}