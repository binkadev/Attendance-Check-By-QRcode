package com.ptithcm.attendapp.view;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log; // THÊM THƯ VIỆN LOG
import android.util.Patterns;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.ptithcm.attendapp.R;

import android.provider.Settings;
import com.ptithcm.attendapp.api.RetrofitClient;
import com.ptithcm.attendapp.model.AuthResponse;
import com.ptithcm.attendapp.model.RegisterRequest;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignUpActivity extends AppCompatActivity {

    // TẠO TAG ĐỂ LỌC LOGCAT
    private static final String TAG = "SignUpActivity_API";

    private ImageView btnBack, btnTogglePass;
    private EditText edtFullName, edtStudentID, edtEmail, edtPassword;
    private MaterialButton btnSignUp;
    private TextView tvLogin;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        initViews();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnTogglePass = findViewById(R.id.btnTogglePass);

        // Ánh xạ đầy đủ các ô nhập liệu
        edtFullName = findViewById(R.id.edtFullName);
        edtStudentID = findViewById(R.id.edtStudentID);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);

        btnSignUp = findViewById(R.id.btnSignUp);
        tvLogin = findViewById(R.id.tvLogin);
    }

    private void setupListeners() {
        // Nút Back và Chữ Đăng nhập: Đóng màn hình này, tự động quay về màn hình Login trước đó
        btnBack.setOnClickListener(v -> finish());
        tvLogin.setOnClickListener(v -> finish());

        // Xử lý nút ẩn/hiện mật khẩu
        btnTogglePass.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                edtPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                btnTogglePass.setImageResource(R.drawable.ic_eye_off);
            } else {
                edtPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                btnTogglePass.setImageResource(R.drawable.ic_eye);
            }
            // Đưa con trỏ nháy về cuối dòng
            edtPassword.setSelection(edtPassword.getText().length());
        });

        // XỬ LÝ SỰ KIỆN BẤM NÚT ĐĂNG KÝ
        btnSignUp.setOnClickListener(v -> processSignUp());
    }

    private void processSignUp() {
        // 1. Lấy dữ liệu người dùng nhập
        String fullName = edtFullName.getText().toString().trim();
        String studentId = edtStudentID.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        // Lấy Device ID của máy
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        RegisterRequest request = new RegisterRequest(email, password, fullName, studentId, deviceId);

        // 2. Kiểm tra tính hợp lệ (Validation)
        if (fullName.isEmpty() || studentId.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Định dạng Email không hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }

        // KIỂM TRA MẬT KHẨU: Ít nhất 10 ký tự, có chứa chữ cái và có chứa số
        if (password.length() < 10 || !password.matches(".*[a-zA-Z].*") || !password.matches(".*\\d.*")) {
            Toast.makeText(this, "Mật khẩu phải từ 10 ký tự, bao gồm cả chữ và số!", Toast.LENGTH_LONG).show();
            return;
        }

        // 3. Hiển thị hiệu ứng Loading
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang tạo tài khoản...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        Log.d(TAG, "Bắt đầu gọi API Đăng ký với Email: " + email);

        // 4. Gọi API Đăng ký thực tế
        RetrofitClient.getApiService().register(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                progressDialog.dismiss(); // Tắt vòng xoay loading

                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Đăng ký thành công!");
                    Toast.makeText(SignUpActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();

                    // Truyền Email về màn hình đăng nhập
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("REGISTERED_EMAIL", email);
                    setResult(RESULT_OK, resultIntent);
                    finish(); // Đóng màn hình đăng ký
                } else {
                    // LẤY LỖI CHÍNH XÁC TỪ SERVER VÀ IN RA LOGCAT
                    try {
                        String errorMsg = response.errorBody() != null ? response.errorBody().string() : "Lỗi không xác định";
                        Log.e(TAG, "Server từ chối (Code " + response.code() + "): " + errorMsg);
                        Toast.makeText(SignUpActivity.this, "Lỗi Server (" + response.code() + "): " + errorMsg, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Log.e(TAG, "Lỗi khi parse errorBody", e);
                        Toast.makeText(SignUpActivity.this, "Đăng ký thất bại. Email hoặc MSSV đã tồn tại!", Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                progressDialog.dismiss();
                Log.e(TAG, "Lỗi kết nối mạng (onFailure): ", t);
                Toast.makeText(SignUpActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}