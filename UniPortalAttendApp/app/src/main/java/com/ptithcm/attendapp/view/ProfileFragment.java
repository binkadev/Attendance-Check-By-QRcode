package com.ptithcm.attendapp.view;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.ptithcm.attendapp.R;
import com.ptithcm.attendapp.api.RetrofitClient;
import com.ptithcm.attendapp.model.UserProfile;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private ImageView btnBack;
    private TextView tvProfileName, tvStudentCode;
    private EditText edtPhone, edtEmail;
    private MaterialButton btnSaveContact, btnLogout;
    private SwitchMaterial switchPushNotif, switchBiometric;
    private LinearLayout btnLanguage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        initViews(view);
        loadUserData();
        setupListeners();
        return view;
    }

    private void initViews(View view) {
        btnBack = view.findViewById(R.id.btnBack);
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvStudentCode = view.findViewById(R.id.tvStudentCode);
        edtPhone = view.findViewById(R.id.edtPhone);
        edtEmail = view.findViewById(R.id.edtEmail);
        btnSaveContact = view.findViewById(R.id.btnSaveContact);
        btnLogout = view.findViewById(R.id.btnLogout);
        switchPushNotif = view.findViewById(R.id.switchPushNotif);
        switchBiometric = view.findViewById(R.id.switchBiometric);
        btnLanguage = view.findViewById(R.id.btnLanguage);
    }

    private void loadUserData() {
        // 1. Lấy Token đã lưu lúc đăng nhập
        SharedPreferences prefs = requireActivity().getSharedPreferences("UniPortalPrefs", Context.MODE_PRIVATE);
        String token = prefs.getString("ACCESS_TOKEN", "");

        if (token.isEmpty()) {
            Toast.makeText(requireContext(), "Lỗi xác thực. Vui lòng đăng nhập lại!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Gọi API /api/v1/me kèm chữ "Bearer " phía trước token
        RetrofitClient.getApiService().getMyProfile("Bearer " + token).enqueue(new Callback<UserProfile>() {
            @Override
            public void onResponse(Call<UserProfile> call, Response<UserProfile> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserProfile user = response.body();

                    if (tvProfileName != null) {
                        tvProfileName.setText(user.getFullName());
                    }

                    if (edtEmail != null) {
                        edtEmail.setText(user.getEmail());
                    }

                    if (tvStudentCode != null) {
                        String code = user.getUserCode();
                        if (code != null && !code.trim().isEmpty()) {
                            tvStudentCode.setText("MSSV: " + code);
                        } else {
                            tvStudentCode.setText("MSSV: Chưa cập nhật");
                        }
                    }

                    // Lưu ngược lại Tên vào SharedPreferences để màn hình HomeFragment dùng chung
                    prefs.edit().putString("USER_NAME", user.getFullName()).apply();

                } else if (response.code() == 401) {
                    Toast.makeText(requireContext(), "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại!", Toast.LENGTH_LONG).show();

                    SharedPreferences prefs = requireActivity().getSharedPreferences("UniPortalPrefs", Context.MODE_PRIVATE);
                    prefs.edit().clear().apply();

                    Intent intent = new Intent(requireActivity(), SignInActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
            }

            @Override
            public void onFailure(Call<UserProfile> call, Throwable t) {
                // Nếu rớt mạng, lấy tạm tên cũ trong bộ nhớ ra xài đỡ
                String savedName = prefs.getString("USER_NAME", "Sinh viên");
                if (tvProfileName != null) {
                    tvProfileName.setText(savedName);
                }
                Toast.makeText(requireContext(), "Không thể tải thông tin mới: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        });

        btnSaveContact.setOnClickListener(v -> {
            String phone = edtPhone.getText().toString();
            String email = edtEmail.getText().toString();
            Toast.makeText(requireContext(), "Đã cập nhật thông tin liên hệ!", Toast.LENGTH_SHORT).show();
            edtPhone.clearFocus();
            edtEmail.clearFocus();
        });

        switchPushNotif.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String status = isChecked ? "Đã bật" : "Đã tắt";
            Toast.makeText(requireContext(), status + " thông báo đẩy", Toast.LENGTH_SHORT).show();
        });

        btnLanguage.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Chọn ngôn ngữ", Toast.LENGTH_SHORT).show();
        });

        btnLogout.setOnClickListener(v -> {
            // Xóa sạch dữ liệu đã lưu khi đăng xuất
            SharedPreferences prefs = requireActivity().getSharedPreferences("UniPortalPrefs", Context.MODE_PRIVATE);
            prefs.edit().clear().apply();

            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}