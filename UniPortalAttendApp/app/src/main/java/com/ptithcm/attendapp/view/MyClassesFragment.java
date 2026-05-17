package com.ptithcm.attendapp.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ptithcm.attendapp.R;
import com.ptithcm.attendapp.api.RetrofitClient;
import com.ptithcm.attendapp.model.ClassItem;
import com.ptithcm.attendapp.model.ClassResponse;
import com.ptithcm.attendapp.model.UpcomingSessionItem;
import com.ptithcm.attendapp.model.UpcomingSessionResponse;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyClassesFragment extends Fragment {

    private LinearLayout layoutDashboard;
    private TextView btnFilterSemester, btnTabJoined, btnTabUnattended;

    private RecyclerView rvClasses;
    private ClassAdapter classAdapter;
    private List<ClassItem> allLoadedClasses = new ArrayList<>();

    // Các View cho màn Dashboard
    private RecyclerView rvTodayClasses, rvTomorrowClasses;
    private TextView tvTodayCount, tvEmptyToday, tvEmptyTomorrow;
    private SessionTodayAdapter todayAdapter;
    private SessionTomorrowAdapter tomorrowAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_classes, container, false);

        initViews(view);
        setupRecyclerViews();
        setupTabListeners();

        // Gọi 2 API cùng lúc
        fetchClassesFromApi();      // Danh sách lớp
        fetchUpcomingSessions();    // Lịch học (Dashboard)

        return view;
    }

    private void initViews(View view) {
        layoutDashboard = view.findViewById(R.id.layoutDashboard);
        btnFilterSemester = view.findViewById(R.id.btnFilterSemester);
        btnTabJoined = view.findViewById(R.id.btnTabJoined);
        btnTabUnattended = view.findViewById(R.id.btnTabUnattended);
        rvClasses = view.findViewById(R.id.rvClasses);

        rvTodayClasses = view.findViewById(R.id.rvTodayClasses);
        rvTomorrowClasses = view.findViewById(R.id.rvTomorrowClasses);
        tvTodayCount = view.findViewById(R.id.tvTodayCount);
        tvEmptyToday = view.findViewById(R.id.tvEmptyToday);
        tvEmptyTomorrow = view.findViewById(R.id.tvEmptyTomorrow);
    }

    private void setupRecyclerViews() {
        // Tab Danh sách chung
        rvClasses.setLayoutManager(new LinearLayoutManager(requireContext()));
        classAdapter = new ClassAdapter(new ArrayList<>(), getActivity().getSupportFragmentManager());
        rvClasses.setAdapter(classAdapter);

        // Dashboard Hôm nay -> Dùng SessionTodayAdapter
        rvTodayClasses.setLayoutManager(new LinearLayoutManager(requireContext()));
        todayAdapter = new SessionTodayAdapter(new ArrayList<>(), getActivity().getSupportFragmentManager());
        rvTodayClasses.setAdapter(todayAdapter);

        // Dashboard Ngày mai -> Dùng SessionTomorrowAdapter
        rvTomorrowClasses.setLayoutManager(new LinearLayoutManager(requireContext()));
        tomorrowAdapter = new SessionTomorrowAdapter(new ArrayList<>(), getActivity().getSupportFragmentManager());
        rvTomorrowClasses.setAdapter(tomorrowAdapter);
    }

    private void setupTabListeners() {
        btnFilterSemester.setOnClickListener(v -> {
            setActiveTab(btnFilterSemester);
            layoutDashboard.setVisibility(View.VISIBLE);
            rvClasses.setVisibility(View.GONE);
        });

        btnTabJoined.setOnClickListener(v -> {
            setActiveTab(btnTabJoined);
            layoutDashboard.setVisibility(View.GONE);
            rvClasses.setVisibility(View.VISIBLE);

            List<ClassItem> joinedClasses = new ArrayList<>();
            for (ClassItem item : allLoadedClasses) {
                String role = item.getMyRole();
                if (role != null && !role.equalsIgnoreCase("OWNER") && !role.equalsIgnoreCase("CREATOR")) {
                    joinedClasses.add(item);
                }
            }
            classAdapter.updateData(joinedClasses);
        });

        btnTabUnattended.setOnClickListener(v -> {
            setActiveTab(btnTabUnattended);
            layoutDashboard.setVisibility(View.GONE);
            rvClasses.setVisibility(View.VISIBLE);

            // Tạm thời hiển thị danh sách trống cho Tab này
            classAdapter.updateData(new ArrayList<>());
        });
    }

    private void setActiveTab(TextView activeTab) {
        btnFilterSemester.setBackgroundResource(R.drawable.bg_chip_inactive);
        btnFilterSemester.setTextColor(Color.parseColor("#0A2540"));
        btnTabJoined.setBackgroundResource(R.drawable.bg_chip_inactive);
        btnTabJoined.setTextColor(Color.parseColor("#0A2540"));
        btnTabUnattended.setBackgroundResource(R.drawable.bg_chip_inactive);
        btnTabUnattended.setTextColor(Color.parseColor("#0A2540"));

        activeTab.setBackgroundResource(R.drawable.bg_chip_active);
        activeTab.setTextColor(Color.WHITE);
    }

    private void fetchClassesFromApi() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UniPortalPrefs", Context.MODE_PRIVATE);
        String token = prefs.getString("ACCESS_TOKEN", "");
        if (token.isEmpty()) return;

        RetrofitClient.getApiService().getMyClasses("Bearer " + token, 0, 50, null)
                .enqueue(new Callback<ClassResponse>() {
                    @Override
                    public void onResponse(Call<ClassResponse> call, Response<ClassResponse> response) {
                        if (!isAdded() || getContext() == null) return;
                        if (response.isSuccessful() && response.body() != null) {
                            allLoadedClasses = response.body().getItems();
                            classAdapter.updateData(allLoadedClasses);
                        }
                    }
                    @Override
                    public void onFailure(Call<ClassResponse> call, Throwable t) {}
                });
    }

    // LOGIC ĐỌC DỮ LIỆU TỪ CẤU TRÚC MỚI (LỌC THEO KEY) VÀ IN LOGCAT KIỂM TRA
    private void fetchUpcomingSessions() {
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

                            // =======================================================
                            // 🚨 [LOGCAT] 1. IN RA TOÀN BỘ JSON GỐC TỪ SERVER
                            // =======================================================
                            try {
                                com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
                                android.util.Log.e("DEBUG_API", "=======================================");
                                android.util.Log.e("DEBUG_API", "🌐 RAW JSON TỪ SERVER TRẢ VỀ:");
                                android.util.Log.e("DEBUG_API", gson.toJson(data));
                                android.util.Log.e("DEBUG_API", "=======================================");
                            } catch (Exception e) {
                                android.util.Log.e("DEBUG_API", "Lỗi in JSON Server: " + e.getMessage());
                            }
                            // =======================================================

                            List<UpcomingSessionItem> todayList = new ArrayList<>();
                            List<UpcomingSessionItem> tomorrowList = new ArrayList<>();

                            // 👇 THÊM ĐOẠN NÀY ĐỂ TÍNH NGÀY HÔM NAY VÀ NGÀY MAI
                            Calendar calToday = Calendar.getInstance();
                            Calendar calTomorrow = Calendar.getInstance();
                            calTomorrow.add(Calendar.DAY_OF_YEAR, 1);

                            SimpleDateFormat sdfLocalYMD = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                            String localTodayStr = sdfLocalYMD.format(calToday.getTime());
                            String localTomorrowStr = sdfLocalYMD.format(calTomorrow.getTime());

                            // 👇 LOGIC LỌC: CHỈ LẤY ĐÚNG HÔM NAY VÀ NGÀY MAI
                            if (data.getSections() != null) {
                                for (com.ptithcm.attendapp.model.UpcomingSection section : data.getSections()) {
                                    String key = section.getKey();

                                    // Backup: Nếu Server không trả key mà trả thẳng Date, ta vẫn check được
                                    if (key == null && section.getDate() != null) {
                                        if (section.getDate().equals(localTodayStr)) {
                                            todayList.addAll(section.getItems());
                                        } else if (section.getDate().equals(localTomorrowStr)) {
                                            tomorrowList.addAll(section.getItems());
                                        }
                                        continue;
                                    }

                                    if (key == null) continue;

                                    // Lọc chuẩn theo Key của Backend
                                    if (key.equalsIgnoreCase("TODAY")) {
                                        if (section.getItems() != null) todayList.addAll(section.getItems());

                                    } else if (key.equalsIgnoreCase("TOMORROW")) {
                                        if (section.getItems() != null) tomorrowList.addAll(section.getItems());

                                    } else if (key.equalsIgnoreCase("UPCOMING")) {
                                        // Gặp mảng UPCOMING, quét tay tìm đúng những môn của ngày mai
                                        if (section.getItems() != null) {
                                            for (UpcomingSessionItem item : section.getItems()) {
                                                if (item.getSessionDate() != null && item.getSessionDate().equals(localTomorrowStr)) {
                                                    tomorrowList.add(item);
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // =======================================================
                            // 🚨 [LOGCAT] 2. IN RA DỮ LIỆU SAU KHI ĐÃ PHÂN LOẠI
                            // =======================================================
                            try {
                                //com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
                                // Thành dòng này (Thêm .serializeNulls()):
                                com.google.gson.Gson gson = new com.google.gson.GsonBuilder().serializeNulls().setPrettyPrinting().create();

                                android.util.Log.i("DEBUG_API", "==== 🔽 DANH SÁCH LỚP HÔM NAY (" + todayList.size() + " lớp) 🔽 ====");
                                android.util.Log.i("DEBUG_API", gson.toJson(todayList));

                                android.util.Log.i("DEBUG_API", "==== 🔽 DANH SÁCH LỚP NGÀY MAI (" + tomorrowList.size() + " lớp) 🔽 ====");
                                android.util.Log.i("DEBUG_API", gson.toJson(tomorrowList));
                                android.util.Log.i("DEBUG_API", "=======================================");
                            } catch (Exception e) {
                                android.util.Log.e("DEBUG_API", "Lỗi in JSON phân loại: " + e.getMessage());
                            }
                            // =======================================================

                            // Cập nhật lên Adapter
                            todayAdapter.updateData(todayList);
                            tomorrowAdapter.updateData(tomorrowList);

                            // Cập nhật vòng tròn đỏ đếm số lớp Hôm nay
                            if (todayList.isEmpty()) {
                                tvTodayCount.setVisibility(View.GONE);
                            } else {
                                tvTodayCount.setVisibility(View.VISIBLE);
                                tvTodayCount.setText(String.valueOf(todayList.size()));
                            }

                            // Ẩn hiện TextView thông báo trống
                            tvEmptyToday.setVisibility(todayList.isEmpty() ? View.VISIBLE : View.GONE);
                            tvEmptyTomorrow.setVisibility(tomorrowList.isEmpty() ? View.VISIBLE : View.GONE);
                        }
                    }

                    @Override
                    public void onFailure(Call<UpcomingSessionResponse> call, Throwable t) {
                        android.util.Log.e("DEBUG_API", "Lỗi mạng khi tải Lịch học: " + t.getMessage());
                    }
                });
    }
}