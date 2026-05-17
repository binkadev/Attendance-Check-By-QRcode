package com.ptithcm.attendapp.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.ptithcm.attendapp.R;
import com.ptithcm.attendapp.api.RetrofitClient;
import com.ptithcm.attendapp.model.UpcomingSection;
import com.ptithcm.attendapp.model.UpcomingSessionItem;
import com.ptithcm.attendapp.model.UpcomingSessionResponse;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private TextView tvUserName;
    private TextView tvGreeting;
    private MaterialButton btnQuickAttend;

    // View cho Lịch trình
    private RecyclerView rvTodaySchedule;
    private TextView tvTodayClassCount;
    private HomeScheduleAdapter adapter;

    // View cho thẻ NEXT CLASS
    private TextView tvNextClassSubject, tvNextClassTeacher, tvNextClassRoom, tvNextClassTime;

    // Khai báo View cho Thống kê
    private TextView tvOverallRate, tvPresentCount, tvLateCount, tvAbsentCount, tvTrend;
    private View viewPresentBar, viewLateBar, viewAbsentBar;
    private LinearLayout layoutProgressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initViews(view);
        setDynamicGreeting();
        loadUserData();
        setupListeners();
        setupRecyclerView();

        // Gọi API lấy lịch học đổ vào Home
        fetchTodaySchedule();

        // Gọi thêm API Thống kê
        fetchAttendanceSummary();

        return view;
    }

    private void initViews(View view) {
        tvUserName = view.findViewById(R.id.tvUserName);
        tvGreeting = view.findViewById(R.id.tvGreeting);
        btnQuickAttend = view.findViewById(R.id.btnQuickAttend);

        rvTodaySchedule = view.findViewById(R.id.rvTodaySchedule);
        tvTodayClassCount = view.findViewById(R.id.tvTodayClassCount);

        tvNextClassSubject = view.findViewById(R.id.tvNextClassSubject);
        tvNextClassTeacher = view.findViewById(R.id.tvNextClassTeacher);
        tvNextClassRoom = view.findViewById(R.id.tvNextClassRoom);
        tvNextClassTime = view.findViewById(R.id.tvNextClassTime);

        // Ánh xạ Thống kê
        tvOverallRate = view.findViewById(R.id.tvOverallRate);
        tvPresentCount = view.findViewById(R.id.tvPresentCount);
        tvLateCount = view.findViewById(R.id.tvLateCount);
        tvAbsentCount = view.findViewById(R.id.tvAbsentCount);
        tvTrend = view.findViewById(R.id.tvTrend);
        
        layoutProgressBar = view.findViewById(R.id.layoutProgressBar);
        viewPresentBar = view.findViewById(R.id.viewPresentBar);
        viewLateBar = view.findViewById(R.id.viewLateBar);
        viewAbsentBar = view.findViewById(R.id.viewAbsentBar);
    }

    private void setupRecyclerView() {
        // Cài đặt RecyclerView cuộn ngang
        rvTodaySchedule.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        adapter = new HomeScheduleAdapter(new ArrayList<>());
        rvTodaySchedule.setAdapter(adapter);
    }

    private void fetchTodaySchedule() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UniPortalPrefs", Context.MODE_PRIVATE);
        String token = prefs.getString("ACCESS_TOKEN", "");
        if (token.isEmpty()) return;

        RetrofitClient.getApiService().getUpcomingSessions("Bearer " + token, 20)
                .enqueue(new Callback<UpcomingSessionResponse>() {
                    @Override
                    public void onResponse(Call<UpcomingSessionResponse> call, Response<UpcomingSessionResponse> response) {
                        if (!isAdded() || getContext() == null) return;
                        if (response.isSuccessful() && response.body() != null) {
                            UpcomingSessionResponse data = response.body();
                            List<UpcomingSessionItem> todayList = new ArrayList<>();

                            if (data.getSections() != null) {
                                for (UpcomingSection section : data.getSections()) {
                                    if ("TODAY".equalsIgnoreCase(section.getKey()) && section.getItems() != null) {
                                        todayList.addAll(section.getItems());
                                    }
                                }
                            }

                            // 1. Cập nhật danh sách kéo ngang
                            adapter.updateData(todayList);
                            tvTodayClassCount.setText(todayList.size() + " Classes");

                            // 2. Cập nhật thẻ NEXT CLASS
                            if (!todayList.isEmpty()) {
                                UpcomingSessionItem nextClass = todayList.get(0);
                                tvNextClassSubject.setText(nextClass.getGroupName() != null ? nextClass.getGroupName() : "Lớp học");
                                tvNextClassTeacher.setText("Giảng viên: " + (nextClass.getLecturerName() != null ? nextClass.getLecturerName() : "Đang cập nhật"));
                                tvNextClassRoom.setText(nextClass.getRoom() != null ? nextClass.getRoom() : "Phòng --");

                                String start = nextClass.getStartTime() != null ? nextClass.getStartTime() : "--:--";
                                String end = nextClass.getEndTime() != null ? nextClass.getEndTime() : "--:--";
                                tvNextClassTime.setText(start + " - " + end);
                            } else {
                                tvNextClassSubject.setText("Hoàn thành mục tiêu!");
                                tvNextClassTeacher.setText("Bạn không có lịch học tiếp theo.");
                                tvNextClassRoom.setText("--");
                                tvNextClassTime.setText("--:--");
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<UpcomingSessionResponse> call, Throwable t) {
                        android.util.Log.e("DEBUG_HOME", "Lỗi tải lịch: " + t.getMessage());
                    }
                });
    }

    private void fetchAttendanceSummary() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UniPortalPrefs", Context.MODE_PRIVATE);
        String token = prefs.getString("ACCESS_TOKEN", "");
        if (token.isEmpty()) return;

        RetrofitClient.getApiService().getMyAttendanceSummary("Bearer " + token)
                .enqueue(new Callback<com.ptithcm.attendapp.model.AttendanceSummaryResponse>() {
                    @Override
                    public void onResponse(Call<com.ptithcm.attendapp.model.AttendanceSummaryResponse> call, Response<com.ptithcm.attendapp.model.AttendanceSummaryResponse> response) {
                        if (!isAdded() || getContext() == null) return;
                        
                        if (response.isSuccessful() && response.body() != null) {
                            com.ptithcm.attendapp.model.AttendanceSummaryResponse data = response.body();

                            // Set Text
                            tvOverallRate.setText(data.getOverallRate() + "%");
                            tvPresentCount.setText(String.valueOf(data.getPresentCount()));
                            tvLateCount.setText(String.valueOf(data.getLateCount()));
                            tvAbsentCount.setText(String.valueOf(data.getAbsentCount()));
                            tvTrend.setText("Cập nhật mới"); // Optional default trend if none provided

                            // Tính toán thanh Progress Bar động
                            int total = data.getPresentCount() + data.getLateCount() + data.getAbsentCount();
                            if (total > 0) {
                                layoutProgressBar.setWeightSum(total);

                                LinearLayout.LayoutParams paramsPresent = (LinearLayout.LayoutParams) viewPresentBar.getLayoutParams();
                                paramsPresent.weight = data.getPresentCount();
                                viewPresentBar.setLayoutParams(paramsPresent);

                                LinearLayout.LayoutParams paramsLate = (LinearLayout.LayoutParams) viewLateBar.getLayoutParams();
                                paramsLate.weight = data.getLateCount();
                                viewLateBar.setLayoutParams(paramsLate);

                                LinearLayout.LayoutParams paramsAbsent = (LinearLayout.LayoutParams) viewAbsentBar.getLayoutParams();
                                paramsAbsent.weight = data.getAbsentCount();
                                viewAbsentBar.setLayoutParams(paramsAbsent);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<com.ptithcm.attendapp.model.AttendanceSummaryResponse> call, Throwable t) {
                        android.util.Log.e("DEBUG_HOME", "Lỗi tải Thống kê: " + t.getMessage());
                    }
                });
    }

    private void setDynamicGreeting() {
        if(tvGreeting == null) return;

        Calendar c = Calendar.getInstance();
        int hourOfDay = c.get(Calendar.HOUR_OF_DAY);

        String greeting = "";
        if(hourOfDay >= 3 && hourOfDay < 12) {
            greeting = "☀️ Good Morning,";
        } else if (hourOfDay >= 12 && hourOfDay < 18) {
            greeting = "🌤️ Good Afternoon,";
        } else if (hourOfDay >= 18 && hourOfDay < 22) {
            greeting = "🌙 Good Evening,";
        } else {
            greeting = "🌌 Good Night,";
        }

        tvGreeting.setText(greeting);
    }

    private void loadUserData() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UniPortalPrefs", Context.MODE_PRIVATE);
        String userName = prefs.getString("USER_NAME", "Sinh viên");

        if (tvUserName != null) {
            tvUserName.setText(userName);
        }
    }

    private void setupListeners() {
        if (btnQuickAttend != null) {
            btnQuickAttend.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "Đang mở màn hình điểm danh...", Toast.LENGTH_SHORT).show();
            });
        }
    }
}