package com.ptithcm.attendapp.view;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log; // THÊM IMPORT NÀY
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.ptithcm.attendapp.R;
import com.ptithcm.attendapp.api.RetrofitClient;
import com.ptithcm.attendapp.model.CheckinResultResponse;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AttendanceSuccessFragment extends Fragment {

    private static final String TAG = "CHECKIN_RESULT_UI"; // TAG ĐỂ TÌM TRONG LOGCAT

    private ImageView btnBack;
    private MaterialButton btnDone, btnScanAgain;
    private TextView tvClassName, tvClassId, tvTime, tvLocation, tvStatusLabel, tvLocationSubtitle;
    private String sessionId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_attendance_success, container, false);
        initViews(view);
        setupListeners();

        Log.d(TAG, "onCreateView: Khởi tạo màn hình thành công");

        if (getArguments() != null) {
            sessionId = getArguments().getString("SCANNED_SESSION_ID");
            Log.d(TAG, "Lấy được sessionId từ Arguments: " + sessionId);

            if (sessionId != null && !sessionId.isEmpty()) {
                fetchCheckinResult();
            } else {
                Log.e(TAG, "SessionId bị rỗng!");
            }
        } else {
            Log.e(TAG, "Không nhận được Arguments nào từ Activity truyền sang!");
        }

        return view;
    }

    private void initViews(View view) {
        btnBack = view.findViewById(R.id.btnBack);
        btnDone = view.findViewById(R.id.btnDone);
        btnScanAgain = view.findViewById(R.id.btnScanAgain);
        tvClassName = view.findViewById(R.id.tvClassName);
        tvClassId = view.findViewById(R.id.tvClassId);
        tvTime = view.findViewById(R.id.tvTime);
        tvLocation = view.findViewById(R.id.tvLocation);
        tvStatusLabel = view.findViewById(R.id.tvStatusLabel);
        tvLocationSubtitle = view.findViewById(R.id.tvLocationSubtitle);
    }

    private void fetchCheckinResult() {
        Log.d(TAG, "Bắt đầu gọi API getCheckinResult cho Session: " + sessionId);

        SharedPreferences prefs = requireActivity().getSharedPreferences("UniPortalPrefs", Context.MODE_PRIVATE);
        String token = prefs.getString("ACCESS_TOKEN", "");

        RetrofitClient.getApiService().getCheckinResult("Bearer " + token, sessionId)
                .enqueue(new Callback<CheckinResultResponse>() {
                    @Override
                    public void onResponse(Call<CheckinResultResponse> call, Response<CheckinResultResponse> response) {
                        if (!isAdded()) return;

                        Log.d(TAG, "API Response HTTP Code: " + response.code());

                        if (response.isSuccessful() && response.body() != null) {
                            Log.d(TAG, "API trả về dữ liệu thành công!");
                            updateUI(response.body());
                        } else {
                            // In lỗi chi tiết từ Server
                            try {
                                String errorBody = response.errorBody() != null ? response.errorBody().string() : "Empty";
                                Log.e(TAG, "Lỗi từ Server: " + errorBody);
                            } catch (Exception e) {
                                Log.e(TAG, "Lỗi khi đọc errorBody", e);
                            }
                            Toast.makeText(getContext(), "Không thể lấy chi tiết kết quả. Mã lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<CheckinResultResponse> call, Throwable t) {
                        if (!isAdded()) return;
                        Log.e(TAG, "Lỗi kết nối mạng hoặc Crash App: ", t);
                        Toast.makeText(getContext(), "Lỗi kết nối mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUI(CheckinResultResponse data) {
        Log.d(TAG, "Đang parse dữ liệu lên UI...");
        Log.d(TAG, "Dữ liệu Môn học: " + data.getSubjectName());
        Log.d(TAG, "Dữ liệu Trạng thái điểm danh: " + data.getAttendanceStatus());
        Log.d(TAG, "Dữ liệu Chuỗi thời gian gốc từ Server: " + data.getCheckInAt());

        // Cập nhật thông tin Lớp học
        tvClassName.setText(data.getSubjectName());
        tvClassId.setText("Mã lớp: " + data.getDisplayCode());

        // Cập nhật Địa điểm
        tvLocation.setText(data.getLocationDisplay());
        tvLocationSubtitle.setText(data.getLocationSubtitle());

        // Cập nhật Thời gian (Parse chuỗi ISO-8601 sang chuẩn Việt Nam)
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = inputFormat.parse(data.getCheckInAt());

            SimpleDateFormat outputFormat = new SimpleDateFormat("hh:mm a - dd/MM/yyyy", Locale.getDefault());
            String formattedDate = outputFormat.format(date);

            Log.d(TAG, "Parse thời gian thành công: " + formattedDate);
            tvTime.setText(formattedDate);
        } catch (Exception e) {
            Log.e(TAG, "Lỗi parse thời gian, sẽ hiển thị chuỗi gốc. Chi tiết lỗi: ", e);
            tvTime.setText(data.getCheckInAt()); // Hiển thị chuỗi gốc nếu parse lỗi
        }

        // Cập nhật Trạng thái (Đúng giờ hay Đi trễ)
        String status = data.getAttendanceStatus();
        tvStatusLabel.setText(data.getAttendanceStatusLabel());

        if ("PRESENT".equalsIgnoreCase(status)) {
            tvStatusLabel.setTextColor(Color.parseColor("#10B981")); // Màu xanh ngọc (Đúng giờ)
        } else if ("LATE".equalsIgnoreCase(status)) {
            tvStatusLabel.setTextColor(Color.parseColor("#F59E0B")); // Màu vàng cam (Đi trễ)
        } else {
            tvStatusLabel.setTextColor(Color.parseColor("#EF4444")); // Màu đỏ (Vắng)
        }
    }

    private void setupListeners() {
        View.OnClickListener goBackListener = v -> {
            Log.d(TAG, "Nút Back/Done được bấm");
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                // Thay bằng Fragment trang chủ thực tế của bạn
                // requireActivity().getSupportFragmentManager().beginTransaction()
                //        .replace(R.id.fragment_container, new HomeFragment())
                //        .commit();
            }
        };
        btnBack.setOnClickListener(goBackListener);
        btnDone.setOnClickListener(goBackListener);

        btnScanAgain.setOnClickListener(v -> {
            Log.d(TAG, "Nút Scan Again được bấm");
            // Đóng Fragment hiện tại và mở lại Activity quét QR
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
            Intent intent = new Intent(requireContext(), QRScannerActivity.class);
            startActivity(intent);
        });
    }
}