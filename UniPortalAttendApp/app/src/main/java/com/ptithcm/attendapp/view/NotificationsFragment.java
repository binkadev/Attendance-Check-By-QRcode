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
import com.ptithcm.attendapp.model.MarkAllReadResponse;
import com.ptithcm.attendapp.model.NotificationItem;
import com.ptithcm.attendapp.model.NotificationResponse;
import com.ptithcm.attendapp.model.UnreadCountResponse;

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

    private void initViews(View view) {
        btnBack = view.findViewById(R.id.btnBack);
        btnMarkAllReadTop = view.findViewById(R.id.btnMarkAllReadTop);
        btnMarkAllReadText = view.findViewById(R.id.btnMarkAllReadText);
        rvNotifications = view.findViewById(R.id.rvNotifications);
    }

    private void setupRecyclerView() {
        rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NotificationAdapter(apiList, (item, position) -> {
            // Khi user click vào thông báo từ API
            if (!item.isRead()) {
                item.setRead(true);
                adapter.notifyItemChanged(position); // Update UI liền cho mượt

                // Gọi API ngầm báo Server đã đọc
                RetrofitClient.getApiService().markAsRead(authToken, item.getId()).enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> call, Response<Void> response) {}
                    @Override public void onFailure(Call<Void> call, Throwable t) {}
                });
            }
        });
        rvNotifications.setAdapter(adapter);
    }

    private void fetchNotifications() {
        RetrofitClient.getApiService().getNotifications(authToken, 0, 20).enqueue(new Callback<NotificationResponse>() {
            @Override
            public void onResponse(Call<NotificationResponse> call, Response<NotificationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    apiList.clear();
                    apiList.addAll(response.body().getItems());
                    adapter.notifyDataSetChanged(); // Đẩy dữ liệu mới vào List bên dưới thẻ giả lập
                } else {
                    Log.e("NOTI_DEBUG", "Lỗi lấy data: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<NotificationResponse> call, Throwable t) {
                Log.e("NOTI_DEBUG", "Lỗi mạng: " + t.getMessage());
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
                    if (response.isSuccessful()) {
                        Toast.makeText(requireContext(), "Đã đánh dấu tất cả là đã đọc", Toast.LENGTH_SHORT).show();
                        // Load lại list thông báo
                        fetchNotifications();
                    }
                }
                @Override
                public void onFailure(Call<MarkAllReadResponse> call, Throwable t) {
                    Toast.makeText(requireContext(), "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                }
            });
        };

        btnMarkAllReadTop.setOnClickListener(markReadListener);
        btnMarkAllReadText.setOnClickListener(markReadListener);
    }
}