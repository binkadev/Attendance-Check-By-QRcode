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

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationsFragment extends Fragment {

    private ImageView btnBack, btnMarkAllReadTop;
    private TextView btnMarkAllReadText, tvUnreadCountLabel;
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
            if (!item.isRead()) {
                item.setRead(true);
                adapter.notifyItemChanged(position);

                RetrofitClient.getApiService().markAsRead(authToken, item.getId()).enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> call, Response<Void> response) {}
                    @Override public void onFailure(Call<Void> call, Throwable t) {}
                });
            }

            handleNotificationClick(item);
        });
        rvNotifications.setAdapter(adapter);
    }

    private void handleNotificationClick(NotificationItem item) {
        String type = item.getType();
        if (type == null) {
            // Thay thế hàm cũ
            showModernBottomSheet(item.getTitle(), item.getBody(), false);
            return;
        }

        switch (type) {
            case "CHECKIN_SUCCESS":
            case "CHECKIN_FAILED":
                if (item.getSessionId() != null) {
                    fetchAndShowCheckinResult(item.getSessionId());
                } else {
                    // Thay thế hàm cũ
                    showModernBottomSheet(item.getTitle(), item.getBody(), false);
                }
                break;

            case "FRAUD_DETECTED":
            case "FRAUD_INCIDENT":
                // Bật true để chữ màu đỏ
                showModernBottomSheet(item.getTitle(), item.getBody() + "\n\nVui lòng liên hệ giảng viên nếu có sai sót.", true);
                break;

            default:
                // Thay thế hàm cũ
                showModernBottomSheet(item.getTitle(), item.getBody(), false);
                break;
        }
    }

    private void fetchAndShowCheckinResult(String sessionId) {
        RetrofitClient.getApiService().getCheckinResult(authToken, sessionId).enqueue(new Callback<CheckinResultResponse>() {
            @Override
            public void onResponse(Call<CheckinResultResponse> call, Response<CheckinResultResponse> response) {
                if (!isAdded() || getContext() == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    CheckinResultResponse result = response.body();

                    // Chuẩn hóa thời gian tại đây
                    String rawTime = result.getCheckInAt();
                    String formattedTime = formatDateTime(rawTime);

                    String message = "Môn học: " + (result.getSubjectName() != null ? result.getSubjectName() : "Không rõ") + "\n"
                            + "Vị trí: " + (result.getLocationDisplay() != null ? result.getLocationDisplay() : result.getRoom()) + "\n"
                            + "Trạng thái: " + (result.getAttendanceStatusLabel() != null ? result.getAttendanceStatusLabel() : result.getAttendanceStatus()) + "\n"
                            + "Thời gian: " + formattedTime + "\n" // Sử dụng thời gian đã chuẩn hóa
                            + "Ghi chú: " + (result.getMessage() != null ? result.getMessage() : "");

                    showModernBottomSheet(result.getTitle() != null ? result.getTitle() : "Kết quả điểm danh", message, false);
                } else {
                    Toast.makeText(getContext(), "Không thể tải chi tiết điểm danh", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<CheckinResultResponse> call, Throwable t) {
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(getContext(), "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String formatDateTime(String isoDateString) {
        if (isoDateString == null || isoDateString.isEmpty()) return "N/A";
        try {
            // Hỗ trợ cả định dạng có .SSSZ và không có
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            // Cắt bỏ phần nano-giây (nếu có) để tránh lỗi parse
            if (isoDateString.contains(".")) {
                isoDateString = isoDateString.substring(0, isoDateString.indexOf("."));
            }

            Date date = inputFormat.parse(isoDateString);
            SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm:ss - dd/MM/yyyy", Locale.getDefault());
            return outputFormat.format(date);
        } catch (Exception e) {
            Log.e("NOTI_DEBUG", "Lỗi format thời gian: " + e.getMessage());
            return isoDateString; // Trả về chuỗi gốc nếu lỗi
        }
    }

    private void showModernBottomSheet(String title, String message, boolean isWarning) {
        if (getContext() == null) return;

//        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext(), R.style.Theme_Design_Light_BottomSheetDialog);
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext(), R.style.CustomBottomSheetDialog);

        View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_bottom_sheet_noti, null);
        bottomSheetDialog.setContentView(view);

        TextView tvTitle = view.findViewById(R.id.bsTitle);
        TextView tvMessage = view.findViewById(R.id.bsMessage);
        View btnClose = view.findViewById(R.id.bsBtnClose);

        tvTitle.setText(title);
        tvMessage.setText(message);

        if (isWarning) {
            tvTitle.setText("⚠️ " + title);
            tvTitle.setTextColor(android.graphics.Color.parseColor("#DC2626"));
        }

        btnClose.setOnClickListener(v -> bottomSheetDialog.dismiss());

        bottomSheetDialog.show();
    }

    private void fetchNotifications() {
        if (swipeRefresh != null) {
            swipeRefresh.setRefreshing(true);
        }

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

                    if (response.body().getItems() != null) {
                        apiList.addAll(response.body().getItems());
                        Log.d("NOTI_DEBUG", "Thành công! Số lượng thông báo tải về: " + apiList.size());
                    } else {
                        Log.d("NOTI_DEBUG", "Dữ liệu items từ server trả về bị null!");
                    }

                    adapter.notifyDataSetChanged();

                    if(apiList.isEmpty()){
                        Log.d("NOTI_DEBUG", "Danh sách thông báo trống!");
                    }

                } else {
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

                Log.e("NOTI_DEBUG", "Lỗi mạng hoặc lỗi Model JSON: " + t.getMessage());
                Toast.makeText(getContext(), "Không thể kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new HomeFragment())
                        .commit();
            }
        });

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

        if(swipeRefresh != null){
            swipeRefresh.setOnRefreshListener(() -> {
                fetchNotifications();
            });
        }
    }
}