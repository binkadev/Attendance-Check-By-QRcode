package com.ptithcm.attendapp.api;

import com.ptithcm.attendapp.model.AttendanceHistoryResponse;
import com.ptithcm.attendapp.model.AttendanceSummaryResponse;
import com.ptithcm.attendapp.model.AuthResponse;
import com.ptithcm.attendapp.model.CheckInQrResponse;
import com.ptithcm.attendapp.model.CheckinResultResponse;
import com.ptithcm.attendapp.model.ClassResponse;
import com.ptithcm.attendapp.model.GroupDetail;
import com.ptithcm.attendapp.model.JoinGroupRequest;
import com.ptithcm.attendapp.model.JoinGroupResponse;
import com.ptithcm.attendapp.model.LoginRequest;
import com.ptithcm.attendapp.model.MarkAllReadResponse;
import com.ptithcm.attendapp.model.NotificationResponse;
import com.ptithcm.attendapp.model.RegisterRequest;
import com.ptithcm.attendapp.model.UnreadCountResponse;
import com.ptithcm.attendapp.model.UpcomingSessionItem;
import com.ptithcm.attendapp.model.UpcomingSessionResponse;
import com.ptithcm.attendapp.model.UserProfile;
import com.ptithcm.attendapp.model.QrCheckInRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    @POST("api/v1/auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);

    @POST("api/v1/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @GET("api/v1/me")
    Call<UserProfile> getMyProfile(@Header("Authorization") String token);

    @GET("api/v1/me/classes")
    Call<ClassResponse> getMyClasses(
            @Header("Authorization") String token,
            @Query("page") int page,
            @Query("size") int size,
            @Query("memberStatus") String memberStatus // Trạng thái: JOINED, PENDING...
    );

    @GET("api/v1/groups/{groupId}")
    Call<GroupDetail> getGroupDetail(
            @Header("Authorization") String token,
            @Path("groupId") String groupId
    );

    @POST("api/v1/groups/join")
    Call<JoinGroupResponse> joinGroup(
            @Header("Authorization") String token,
            @Body JoinGroupRequest request
    );


    @POST("api/v1/sessions/{sessionId}/checkin/qr")
    Call<CheckInQrResponse> checkinWithQr(
            @Header("Authorization") String token,
            @Path("sessionId") String sessionId,
            @Body QrCheckInRequest requestBody
    );

    // API lay ket qua diem danh sinh vien
    @GET("api/v1/sessions/{sessionId}/me/checkin-result")
    Call<CheckinResultResponse> getCheckinResult(
            @Header("Authorization") String token,
            @Path("sessionId") String sessionId
    );



    @GET("api/v1/me/notifications")
    Call<NotificationResponse> getNotifications(
            @Header("Authorization") String token,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("api/v1/me/notifications/unread-count")
    Call<UnreadCountResponse> getUnreadCount(@Header("Authorization") String token);

    @POST("api/v1/me/notifications/{notificationId}/read")
    Call<Void> markAsRead(
            @Header("Authorization") String token,
            @Path("notificationId") String notificationId
    );

    @POST("api/v1/me/notifications/read-all")
    Call<MarkAllReadResponse> markAllAsRead(@Header("Authorization") String token);

    @GET("api/v1/groups/{groupId}/me/attendance-history")
    Call<AttendanceHistoryResponse> getAttendanceHistory(
            @Header("Authorization") String token,
            @Path("groupId") String groupId,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("api/v1/me/sessions/upcoming")
    Call<UpcomingSessionResponse> getUpcomingSessions(
            @Header("Authorization") String token,
            @Query("limit") int limit
    );

    // TODO: Bạn có thể thêm logout và refresh token sau

    @GET("api/v1/me/attendance/summary")
    Call<AttendanceSummaryResponse> getMyAttendanceSummary(@Header("Authorization") String token);
}