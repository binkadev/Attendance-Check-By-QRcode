package com.ptithcm.attendapp.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ptithcm.attendapp.R;
import com.ptithcm.attendapp.api.RetrofitClient;
import com.ptithcm.attendapp.model.CheckinResultResponse;
import com.ptithcm.attendapp.model.MarkAllReadResponse;
import com.ptithcm.attendapp.model.NotificationItem;
import com.ptithcm.attendapp.model.NotificationResponse;
import com.ptithcm.attendapp.model.UnreadCountResponse;

import android.app.AlertDialog;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;


import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationsFragment extends Fragment {

    private ImageView btnBack, btnMarkAllReadTop;
    private TextView btnMarkAllReadText, tvUnreadCountLabel; // Thêm ID cho Text đếm số ở XML nếu bạn muốn đổi động
    private RecyclerView rvNotifications;

    private NotificationAdapter adapter;
    private List<NotificationItem> apiList = new ArrayList<>();
    private String authToken;

    private SwipeRefreshLayout swipeRefresh;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        SharedPreferences prefs = requireActivity().getSharedPreferences("UniPortalPrefs", Context.MODE_PRIVATE);
        authToken = "Bearer " + prefs.getString("ACCESS_TOKEN", "");

        initViews(view);
        setupRecyclerView();
        setupListeners();

        // Gọi API
        fetchNotifications();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Tự động gọi lại API để làm mới danh sách mỗi khi mở lại màn hình này
        if (authToken != null && !authToken.isEmpty()) {
            fetchNotifications();
        }
    }

    private void initViews(View view) {
        btnBack = view.findViewById(R.id.btnBack);
        btnMarkAllReadTop = view.findViewById(R.id.btnMarkAllReadTop);
        btnMarkAllReadText = view.findViewById(R.id.btnMarkAllReadText);
        rvNotifications = view.findViewById(R.id.rvNotifications);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
    }

    private void setupRecyclerView() {
        rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NotificationAdapter(apiList, (item, position) -> {
            // 1. Logic đánh dấu đã đọc (Giữ nguyên)
            if (!item.isRead()) {
                item.setRead(true);
                adapter.notifyItemChanged(position);

                RetrofitClient.getApiService().markAsRead(authToken, item.getId()).enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> call, Response<Void> response) {}
                    @Override public void onFailure(Call<Void> call, Throwable t) {}
                });
            }

            // 2. Logic xử lý khi click vào để xem chi tiết
            handleNotificationClick(item);
        });
        rvNotifications.setAdapter(adapter);
    }

    private void handleNotificationClick(NotificationItem item) {
        String type = item.getType();
        if (type == null) {
            showSimpleDialog(item.getTitle(), item.getBody());
            return;
        }

        switch (type) {
            case "CHECKIN_SUCCESS":
            case "CHECKIN_FAILED":
                // Gọi API lấy kết quả điểm danh thực tế dựa vào sessionId
                if (item.getSessionId() != null) {
                    fetchAndShowCheckinResult(item.getSessionId());
                } else {
                    showSimpleDialog(item.getTitle(), item.getBody());
                }
                break;

            case "FRAUD_DETECTED":
            case "FRAUD_INCIDENT":
                // Nếu là gian lận, có thể payload chứa chi tiết, hoặc hiển thị cảnh báo đỏ
                showFraudWarningDialog(item.getTitle(), item.getBody());
                break;

            default:
                // Các loại thông báo thông thường khác
                showSimpleDialog(item.getTitle(), item.getBody());
                break;
        }
    }

    // Gọi API chi tiết kết quả điểm danh
    // Thay thế hàm này trong NotificationsFragment.java
    private void fetchAndShowCheckinResult(String sessionId) {
        RetrofitClient.getApiService().getCheckinResult(authToken, sessionId).enqueue(new Callback<CheckinResultResponse>() {
            @Override
            public void onResponse(Call<CheckinResultResponse> call, Response<CheckinResultResponse> response) {
                // Kiểm tra an toàn chống crash
                if (!isAdded() || getContext() == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    CheckinResultResponse result = response.body();

                    // Lấy dữ liệu từ các hàm getter đúng chuẩn của Model
                    String title = result.getTitle() != null ? result.getTitle() : "Kết quả điểm danh";

                    String status = result.getAttendanceStatusLabel() != null ?
                            result.getAttendanceStatusLabel() : result.getAttendanceStatus();
                    String checkInTime = result.getCheckInAt() != null ? result.getCheckInAt() : "N/A";
                    String subject = result.getSubjectName() != null ? result.getSubjectName() : "Không rõ";
                    String roomInfo = result.getLocationDisplay() != null ? result.getLocationDisplay() : result.getRoom();
                    String msg = result.getMessage() != null ? result.getMessage() : "";

                    // Format lại thông điệp hiển thị
                    String message = "Môn học: " + subject + "\n"
                            + "Vị trí: " + roomInfo + "\n"
                            + "Trạng thái: " + status + "\n"
                            + "Thời gian: " + checkInTime + "\n"
                            + "Ghi chú: " + msg;

                    showSimpleDialog(title, message);
                } else {
                    Toast.makeText(getContext(), "Không thể tải chi tiết điểm danh", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<CheckinResultResponse> call, Throwable t) {
                // Kiểm tra an toàn chống crash
                if (!isAdded() || getContext() == null) return;

                Toast.makeText(getContext(), "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Dialog hiển thị cảnh báo gian lận (Custom UI dọa user xíu)
    private void showFraudWarningDialog(String title, String message) {
        new AlertDialog.Builder(getContext())
                .setTitle("⚠️ " + title)
                .setMessage(message + "\n\nVui lòng liên hệ giảng viên nếu có sai sót.")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Đã hiểu", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // Dialog hiển thị thông báo cơ bản
    private void showSimpleDialog(String title, String message) {
        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Đóng", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void fetchNotifications() {
        if (swipeRefresh != null) {
            swipeRefresh.setRefreshing(true);
        }

        // In thử token ra Logcat xem có bị lỗi "Bearer Bearer" không
        Log.d("NOTI_DEBUG", "Token đang dùng: " + authToken);

        RetrofitClient.getApiService().getNotifications(authToken, 0, 20).enqueue(new Callback<NotificationResponse>() {
            @Override
            public void onResponse(Call<NotificationResponse> call, Response<NotificationResponse> response) {
                if (!isAdded() || getContext() == null) return;

                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(false);
                }

                if (response.isSuccessful() && response.body() != null) {
                    apiList.clear();

                    // CHỐNG CRASH KHI SERVER TRẢ VỀ NULL ITEMS
                    if (response.body().getItems() != null) {
                        apiList.addAll(response.body().getItems());
                    }

                    adapter.notifyDataSetChanged();

                    // Nếu danh sách trống, có thể log ra để biết
                    if(apiList.isEmpty()){
                        Log.d("NOTI_DEBUG", "Danh sách thông báo trống!");
                    }

                } else {
                    // In thêm errorBody để biết server mắng gì (ví dụ: Token hết hạn, sai định dạng...)
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Không rõ lỗi";
                        Log.e("NOTI_DEBUG", "Lỗi lấy data Code: " + response.code() + " | Chi tiết: " + errorBody);
                        Toast.makeText(getContext(), "Lỗi Server: " + response.code(), Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<NotificationResponse> call, Throwable t) {
                if (!isAdded() || getContext() == null) return;

                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(false);
                }

                // Nếu nhảy vào đây, 90% là do Model Java không khớp với JSON của Server (Lỗi Parse GSON)
                Log.e("NOTI_DEBUG", "Lỗi mạng hoặc lỗi Model JSON: " + t.getMessage());
                Toast.makeText(getContext(), "Không thể kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        // Nút Back
        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new HomeFragment())
                        .commit();
            }
        });

        // Nút đánh dấu đã đọc tất cả
        View.OnClickListener markReadListener = v -> {
            RetrofitClient.getApiService().markAllAsRead(authToken).enqueue(new Callback<MarkAllReadResponse>() {
                @Override
                public void onResponse(Call<MarkAllReadResponse> call, Response<MarkAllReadResponse> response) {
                    if (!isAdded() || getContext() == null) return;

                    if (response.isSuccessful()) {
                        Toast.makeText(getContext(), "Đã đánh dấu tất cả là đã đọc", Toast.LENGTH_SHORT).show();
                        fetchNotifications();
                    }
                }
                @Override
                public void onFailure(Call<MarkAllReadResponse> call, Throwable t) {
                    if (!isAdded() || getContext() == null) return;
                    Toast.makeText(getContext(), "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                }
            });
        };

        btnMarkAllReadTop.setOnClickListener(markReadListener);
        btnMarkAllReadText.setOnClickListener(markReadListener);

        // BẠN ĐÃ QUÊN ĐOẠN NÀY LẦN TRƯỚC: Bật sự kiện vuốt để làm mới
        if(swipeRefresh != null){
            swipeRefresh.setOnRefreshListener(() -> {
                fetchNotifications();
            });
        }
    }
}