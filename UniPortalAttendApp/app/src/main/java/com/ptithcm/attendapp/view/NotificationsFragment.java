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

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
            showModernBottomSheet(item.getTitle(), item.getBody(), null, "INFO");
            return;
        }

        // Bóc tách payload chung (nếu có)
        String details = parsePayloadToText(item);

        switch (type) {
            case "CHECKIN_SUCCESS":
            case "CHECKIN_FAILED":
                if (item.getSessionId() != null) {
                    fetchAndShowCheckinResult(item.getSessionId());
                } else {
                    showModernBottomSheet(item.getTitle(), item.getBody(), details, type);
                }
                break;

            case "FRAUD_DETECTED":
            case "FRAUD_INCIDENT":
                showModernBottomSheet(item.getTitle(), item.getBody() + "\n\nVui lòng liên hệ giảng viên nếu có sai sót.", details, type);
                break;

            case "ATTENDANCE_POLICY_EXAM_BANNED":
            case "ATTENDANCE_POLICY_CRITICAL":
                // Cảnh báo đỏ, cấm thi
                showModernBottomSheet(item.getTitle(), item.getBody() + "\n\nVui lòng liên hệ phòng đào tạo hoặc giảng viên ngay lập tức để được hỗ trợ.", details, type);
                break;

            case "ATTENDANCE_POLICY_WARNING":
                // Cảnh báo cam
                showModernBottomSheet(item.getTitle(), item.getBody() + "\n\nHãy chú ý đi học đầy đủ để tránh bị cấm thi nhé.", details, type);
                break;

            default:
                showModernBottomSheet(item.getTitle(), item.getBody(), details, type);
                break;
        }
    }

    private String parsePayloadToText(NotificationItem item) {
        Map<String, Object> payload = null;

        try {
            // Lấy dữ liệu dạng Object gốc từ model
            Object rawPayload = item.getPayload();

            // Kiểm tra an toàn: Nếu nó thực sự là một cấu trúc Map (JSON Object) thì mới ép kiểu
            if (rawPayload instanceof Map) {
                payload = (Map<String, Object>) rawPayload;
            }
        } catch (Exception e) {
            return null;
        }

        if (payload == null || payload.isEmpty()) return null;

        StringBuilder sb = new StringBuilder();

        // Kiểm tra và in các chỉ số chuyên cần (Dùng hàm format để bỏ ".0" thừa)
        if (payload.containsKey("absentCount")) {
            sb.append("• Số buổi vắng: ").append(formatNumber(payload.get("absentCount"))).append(" buổi\n");
        }
        if (payload.containsKey("presentCount")) {
            sb.append("• Số buổi có mặt: ").append(formatNumber(payload.get("presentCount"))).append(" buổi\n");
        }
        if (payload.containsKey("attendanceRate")) {
            sb.append("• Tỉ lệ đi học: ").append(formatNumber(payload.get("attendanceRate"))).append("%\n");
        }

        // CHUẨN HÓA TRẠNG THÁI SANG TIẾNG VIỆT TẠI ĐÂY
        if (payload.containsKey("policyStatus")) {
            String rawStatus = String.valueOf(payload.get("policyStatus"));
            String viStatus = getPolicyStatusVietnamese(rawStatus); // Gọi hàm dịch
            sb.append("• Trạng thái hệ thống: ").append(viStatus).append("\n");
        }

        // Bắt các thông số khác nếu cần thiết
        if (payload.containsKey("examBanAbsentCount")) {
            sb.append("• Mức trần cấm thi: ").append(formatNumber(payload.get("examBanAbsentCount"))).append(" buổi\n");
        }

        return sb.toString().trim();
    }

    // Hàm phụ trợ giúp hiển thị số đẹp hơn (vd: 2.0 -> 2, 50.5 -> 50.5)
    private String formatNumber(Object value) {
        if (value == null) return "0";
        String strValue = String.valueOf(value);
        if (strValue.endsWith(".0")) {
            return strValue.substring(0, strValue.length() - 2);
        }
        return strValue;
    }

    public static String getPolicyStatusVietnamese(String status) {
        if (status == null) return "Chưa có dữ liệu";

        switch (status.toUpperCase()) {
            case "EXAM_BANNED":
            case "ATTENDANCE_POLICY_EXAM_BANNED":
                return "Bị cấm thi ❌";

            case "CRITICAL":
            case "ATTENDANCE_POLICY_CRITICAL":
                return "Nguy cơ cấm thi nghiêm trọng 🚨";

            case "WARNING":
            case "ATTENDANCE_POLICY_WARNING":
                return "Cảnh báo chuyên cần ⚠️";

            case "NORMAL":
            case "NORMAL_DIEMDANH":
                return "Đủ điều kiện dự thi ✅";

            default:
                return status; // Trả về chuỗi gốc nếu không khớp để tránh mất dữ liệu
        }
    }

    private void fetchAndShowCheckinResult(String sessionId) {
        RetrofitClient.getApiService().getCheckinResult(authToken, sessionId).enqueue(new Callback<CheckinResultResponse>() {
            @Override
            public void onResponse(Call<CheckinResultResponse> call, Response<CheckinResultResponse> response) {
                if (!isAdded() || getContext() == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    CheckinResultResponse result = response.body();

                    String rawTime = result.getCheckInAt();
                    String formattedTime = formatDateTime(rawTime);

                    String details = "• Môn học: " + (result.getSubjectName() != null ? result.getSubjectName() : "Không rõ") + "\n"
                            + "• Vị trí: " + (result.getLocationDisplay() != null ? result.getLocationDisplay() : result.getRoom()) + "\n"
                            + "• Thời gian: " + formattedTime;

                    String message = "Trạng thái: " + (result.getAttendanceStatusLabel() != null ? result.getAttendanceStatusLabel() : result.getAttendanceStatus());
                    if (result.getMessage() != null && !result.getMessage().isEmpty()) {
                        message += "\n\nGhi chú: " + result.getMessage();
                    }

                    showModernBottomSheet(result.getTitle() != null ? result.getTitle() : "Kết quả điểm danh", message, details, "CHECKIN_SUCCESS");
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
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            if (isoDateString.contains(".")) {
                isoDateString = isoDateString.substring(0, isoDateString.indexOf("."));
            }

            Date date = inputFormat.parse(isoDateString);
            SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm:ss - dd/MM/yyyy", Locale.getDefault());
            return outputFormat.format(date);
        } catch (Exception e) {
            Log.e("NOTI_DEBUG", "Lỗi format thời gian: " + e.getMessage());
            return isoDateString;
        }
    }

    private void showModernBottomSheet(String title, String message, String details, String notificationType) {
        if (getContext() == null) return;

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext(), R.style.CustomBottomSheetDialog);
        View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_bottom_sheet_noti, null);
        bottomSheetDialog.setContentView(view);

        ImageView ivIcon = view.findViewById(R.id.bsIcon);
        TextView tvTitle = view.findViewById(R.id.bsTitle);
        TextView tvMessage = view.findViewById(R.id.bsMessage);
        View bsDetailsCard = view.findViewById(R.id.bsDetailsCard);
        TextView tvDetailsText = view.findViewById(R.id.bsDetailsText);
        View btnClose = view.findViewById(R.id.bsBtnClose);

        // Set text cơ bản
        tvTitle.setText(title != null ? title : "Thông báo");
        tvMessage.setText(message != null ? message : "");

        // Set chi tiết Payload
        if (details != null && !details.trim().isEmpty()) {
            bsDetailsCard.setVisibility(View.VISIBLE);
            tvDetailsText.setText(details);
        } else {
            bsDetailsCard.setVisibility(View.GONE);
        }

        // Tùy biến UI
        if (notificationType == null) notificationType = "INFO";

        if (notificationType.contains("CHECKIN_SUCCESS")) {
            ivIcon.setImageResource(R.drawable.ic_person_check);
            ivIcon.setColorFilter(android.graphics.Color.parseColor("#10B981"));
            ivIcon.setBackgroundResource(R.drawable.bg_circle_green_light);
            tvTitle.setTextColor(android.graphics.Color.parseColor("#065F46"));

        } else if (notificationType.contains("EXAM_BANNED") || notificationType.contains("CRITICAL") || notificationType.contains("FRAUD")) {
            ivIcon.setImageResource(R.drawable.ic_warning);
            ivIcon.setColorFilter(android.graphics.Color.parseColor("#EF4444"));
            ivIcon.setBackgroundResource(R.drawable.bg_circle_red_light_solid);
            tvTitle.setTextColor(android.graphics.Color.parseColor("#DC2626"));

        } else if (notificationType.contains("WARNING")) {
            ivIcon.setImageResource(R.drawable.ic_warning);
            ivIcon.setColorFilter(android.graphics.Color.parseColor("#F59E0B"));
            ivIcon.setBackgroundResource(R.drawable.bg_circle_orange_light);
            tvTitle.setTextColor(android.graphics.Color.parseColor("#92400E"));

        } else {
            ivIcon.setImageResource(R.drawable.ic_announcement);
            ivIcon.setColorFilter(android.graphics.Color.parseColor("#3B82F6"));
            ivIcon.setBackgroundResource(R.drawable.bg_icon_circle_blue);
            tvTitle.setTextColor(android.graphics.Color.parseColor("#0A2540"));
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

                        com.google.gson.Gson gson = new com.google.gson.GsonBuilder()
                                .setPrettyPrinting()
                                .create();

                        Log.d("SERVER_NOTI_DEBUG", "--- BẮT ĐẦU DANH SÁCH THÔNG BÁO TỪ SERVER ---");
                        Log.d("SERVER_NOTI_DEBUG", "Tổng số thông báo: " + apiList.size());

                        for (int i = 0; i < apiList.size(); i++) {
                            NotificationItem item = apiList.get(i);
                            Log.d("SERVER_NOTI_DEBUG", "--------------------------------");
                            Log.d("SERVER_NOTI_DEBUG", "Item thứ: " + i);
                            Log.d("SERVER_NOTI_DEBUG", "ID: " + item.getId());
                            Log.d("SERVER_NOTI_DEBUG", "Tiêu đề (Title): " + item.getTitle());
                            Log.d("SERVER_NOTI_DEBUG", "Loại (Type): " + item.getType());
                            Log.d("SERVER_NOTI_DEBUG", "Mức độ (Severity): " + item.getSeverity());
                            Log.d("SERVER_NOTI_DEBUG", "Nội dung (Body): " + item.getBody());
                            Log.d("SERVER_NOTI_DEBUG", "Trạng thái đọc (isRead): " + item.isRead());
                            Log.d("SERVER_NOTI_DEBUG", "Giờ tạo (CreatedAt): " + item.getCreatedAt());

                            String prettyJson = gson.toJson(item);
                            Log.d("SERVER_NOTI_DEBUG", "JSON Gốc (Formatted):\n" + prettyJson);
                        }
                        Log.d("SERVER_NOTI_DEBUG", "--- KẾT THÚC DANH SÁCH THÔNG BÁO ---");
                    } else {
                        Log.d("NOTI_DEBUG", "Dữ liệu items từ server trả về bị null!");
                    }

                    adapter.notifyDataSetChanged();

                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Không rõ lỗi";
                        Log.e("SERVER_NOTI_DEBUG", "LỖI SERVER - Code: " + response.code() + " | Chi tiết: " + errorBody);
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