package com.ptithcm.attendapp.view;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log; // THÊM THƯ VIỆN LOG
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.ptithcm.attendapp.R;
import com.ptithcm.attendapp.viewmodel.SignInViewModel;

import android.provider.Settings;
import android.content.SharedPreferences;
import com.ptithcm.attendapp.api.RetrofitClient;
import com.ptithcm.attendapp.model.AuthResponse;
import com.ptithcm.attendapp.model.LoginRequest;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignInActivity extends AppCompatActivity {

    // TẠO TAG ĐỂ LỌC LOGCAT CHO DỄ TÌM
    private static final String TAG = "SignInActivity_API";

    private SignInViewModel viewModel;

    private ImageView btnBack;
    private TextInputEditText edtEmail, edtPassword;
    private TextView tvForgotPassword;
    private MaterialButton btnLogin, btnSSO;

    private TextView tvSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        viewModel = new ViewModelProvider(this).get(SignInViewModel.class);

        initViews();
        setupListeners();
        observeViewModel();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnSSO = findViewById(R.id.btnSSO);
        tvSignUp = findViewById(R.id.tvSignUp);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());

        tvForgotPassword.setOnClickListener(v -> {
            Toast.makeText(this, "Chức năng quên mật khẩu...", Toast.LENGTH_SHORT).show();
        });

        btnLogin.setOnClickListener(v -> {
            String email = edtEmail.getText() != null ? edtEmail.getText().toString().trim() : "";
            String password = edtPassword.getText() != null ? edtPassword.getText().toString().trim() : "";
            String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

            if(email.isEmpty() || password.isEmpty()){
                Toast.makeText(this, "Vui lòng nhập email và mật khẩu", Toast.LENGTH_SHORT).show();
                return;
            }

            // Hiện Loading...
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Đang đăng nhập...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            Log.d(TAG, "Bắt đầu gọi API Login với Email: " + email); // Log báo bắt đầu

            LoginRequest request = new LoginRequest(email, password, deviceId);

            RetrofitClient.getApiService().login(request).enqueue(new Callback<AuthResponse>() {
                @Override
                public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                    // Tắt Loading...
                    progressDialog.dismiss();

                    if (response.isSuccessful() && response.body() != null) {
                        AuthResponse data = response.body();

                        Log.d(TAG, "Đăng nhập thành công! Token: " + data.getAccessToken()); // Log thành công

                        // LƯU TOKEN VÀO BỘ NHỚ MÁY
                        SharedPreferences prefs = getSharedPreferences("UniPortalPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("ACCESS_TOKEN", data.getAccessToken());
                        editor.putString("REFRESH_TOKEN", data.getRefreshToken());
                        editor.putString("USER_NAME", data.getUser().getFullName());
                        editor.apply();

                        Toast.makeText(SignInActivity.this, "Chào mừng " + data.getUser().getFullName(), Toast.LENGTH_SHORT).show();

                        // CHUYỂN SANG MÀN HÌNH CHÍNH
                        Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);

                    } else {
                        try {
                            String errorMsg = response.errorBody() != null ? response.errorBody().string() : "Lỗi không xác định";

                            // IN LỖI ĐỎ RA LOGCAT
                            Log.e(TAG, "Lỗi từ Server (Code " + response.code() + "): " + errorMsg);

                            Toast.makeText(SignInActivity.this, "Lỗi Server (" + response.code() + "): " + errorMsg, Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Log.e(TAG, "Lỗi khi đọc errorBody", e);
                            Toast.makeText(SignInActivity.this, "Đăng nhập thất bại (Mã " + response.code() + ")", Toast.LENGTH_LONG).show();
                        }
                    }
                }

                @Override
                public void onFailure(Call<AuthResponse> call, Throwable t) {
                    progressDialog.dismiss();

                    // IN TOÀN BỘ STACK TRACE LỖI MẠNG RA LOGCAT
                    Log.e(TAG, "Nổ Exception khi gọi API (onFailure): ", t);

                    Toast.makeText(SignInActivity.this, "Lỗi kết nối server!", Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnSSO.setOnClickListener(v -> {
            Toast.makeText(this, "Chuyển hướng Webview đăng nhập SSO...", Toast.LENGTH_SHORT).show();
        });

        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(SignInActivity.this, SignUpActivity.class);
            startActivity(intent);
        });
    }

    private void observeViewModel() {
        viewModel.getLoginState().observe(this, state -> {
            if (state == null) return;

            if (state.isLoading) {
                btnLogin.setEnabled(false);
                btnLogin.setText("Đang xử lý...");
            } else {
                btnLogin.setEnabled(true);
                btnLogin.setText("Đăng nhập");

                if (state.isSuccess) {
                    Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                    startActivity(intent);

                    finish();
                } else if (state.errorMessage != null) {
                    Toast.makeText(this, state.errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}