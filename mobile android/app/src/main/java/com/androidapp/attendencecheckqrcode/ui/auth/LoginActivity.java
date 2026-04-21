package com.androidapp.attendencecheckqrcode.ui.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.androidapp.attendencecheckqrcode.R;
import com.androidapp.attendencecheckqrcode.data.dto.auth.LoginRequest;
import com.androidapp.attendencecheckqrcode.ui.home.HomeActivity;
import com.androidapp.attendencecheckqrcode.utils.TokenManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.androidapp.attendencecheckqrcode.data.dto.auth.ForgotPasswordRequest;
import com.androidapp.attendencecheckqrcode.data.api.ApiClient;
import com.androidapp.attendencecheckqrcode.data.dto.auth.AuthResponse;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private ImageView btnShowPass;
    private TextView tvSignUp, tvForgotPassword;
    private CheckBox cbRemember;
    private boolean isPasswordVisible = false;
    private TokenManager tokenManager;
    private AuthViewModel authViewModel;

    // Tên file lưu trữ thông tin Remember Me
    private static final String PREF_NAME = "LoginPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        tokenManager = new TokenManager(this);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Auto-login nếu đã có Token
        if (tokenManager.getAccessToken() != null) {
            goToHome();
            return;
        }

        initViews();
        loadRememberedCredentials(); // Tải dữ liệu đã lưu nếu có
        setupListeners();
        observeViewModel();
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnShowPass = findViewById(R.id.btnShowPass);
        tvSignUp = findViewById(R.id.tvSignUp);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        cbRemember = findViewById(R.id.cbRemember);
    }

    private void observeViewModel() {
        authViewModel.getLoginResult().observe(this, response -> {
            switch (response.status) {
                case LOADING:
                    btnLogin.setEnabled(false);
                    btnLogin.setText("Đang đăng nhập...");
                    break;
                case SUCCESS:
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Đăng nhập");

                    if (response.data != null && response.data.getAccessToken() != null) {
                        android.util.Log.d("DEBUG_LOGIN", "===> AccessToken: " + response.data.getAccessToken());

                        String accessToken = response.data.getAccessToken();
                        String refreshToken = response.data.getRefreshToken();

                        tokenManager.saveTokens(accessToken, refreshToken);

                        saveCredentials(etEmail.getText().toString().trim(), etPassword.getText().toString().trim());

                        if (response.data.getUser() != null) {
                            String name = response.data.getUser().getFullName();
                            String email = response.data.getUser().getEmail();

                            android.util.Log.d("DEBUG_LOGIN", "===> User Profile: Name = [" + name + "], Email = [" + email + "]");

                            tokenManager.saveUserData(name, email);
                        } else {
                            android.util.Log.e("DEBUG_LOGIN", "===> LỖI: Server trả về SUCCESS nhưng object USER bị NULL!");
                        }

                        Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                        goToHome();
                    } else {
                        android.util.Log.e("DEBUG_LOGIN", "===> THẤT BẠI: response.data null hoặc không có Token");
                        Toast.makeText(this, "Không nhận được Token từ máy chủ!", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case ERROR:
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Đăng nhập");
                    Toast.makeText(this, response.message, Toast.LENGTH_LONG).show();
                    break;
            }
        });
    }

    private void setupListeners() {
        btnShowPass.setOnClickListener(v -> {
            if (isPasswordVisible) {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            } else {
                etPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            }
            etPassword.setSelection(etPassword.getText().length());
            isPasswordVisible = !isPasswordVisible;
        });

        tvSignUp.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, SignUpActivity.class)));

        tvForgotPassword.setOnClickListener(v -> handleForgotPassword());

        btnLogin.setOnClickListener(v -> handleLogin());
    }

    private void loadRememberedCredentials() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean isRemembered = prefs.getBoolean("isRemembered", false);

        if (isRemembered) {
            etEmail.setText(prefs.getString("email", ""));
            etPassword.setText(prefs.getString("password", ""));
            cbRemember.setChecked(true);
        }
    }

    private void saveCredentials(String email, String password) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if (cbRemember.isChecked()) {
            editor.putString("email", email);
            editor.putString("password", password);
            editor.putBoolean("isRemembered", true);
        } else {
            editor.clear();
        }
        editor.apply();
    }

    private void handleForgotPassword() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Khôi phục mật khẩu");
        builder.setMessage("Vui lòng nhập Email của bạn. Chúng tôi sẽ gửi hướng dẫn đặt lại mật khẩu.");

        final EditText inputEmail = new EditText(this);
        inputEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        inputEmail.setHint("Email đăng ký");

        inputEmail.setText(etEmail.getText().toString().trim());

        builder.setView(inputEmail);

        builder.setPositiveButton("Gửi", (dialog, which) -> {
            String resetEmail = inputEmail.getText().toString().trim();
            if (resetEmail.isEmpty()) {
                Toast.makeText(this, "Email không được để trống!", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "Đang gửi yêu cầu cho: " + resetEmail, Toast.LENGTH_SHORT).show();

            // Gọi API
            ForgotPasswordRequest request = new ForgotPasswordRequest(resetEmail);

            ApiClient.getApiService(this).forgotPassword(request).enqueue(new Callback<AuthResponse>() {
                @Override
                public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Đã gửi link khôi phục qua Email!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(LoginActivity.this, "Email không tồn tại hoặc không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<AuthResponse> call, Throwable t) {
                    Toast.makeText(LoginActivity.this, "Lỗi kết nối mạng, vui lòng thử lại!", Toast.LENGTH_SHORT).show();
                }
            });

        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // --- XỬ LÝ ĐĂNG NHẬP CHÍNH ---
    private void handleLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        String deviceId = android.provider.Settings.Secure.getString(
                getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID
        );

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập Email và Mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }

        authViewModel.login(new LoginRequest(email, password, deviceId));
    }

    private void goToHome() {
        startActivity(new Intent(LoginActivity.this, HomeActivity.class));
        finish();
    }
}