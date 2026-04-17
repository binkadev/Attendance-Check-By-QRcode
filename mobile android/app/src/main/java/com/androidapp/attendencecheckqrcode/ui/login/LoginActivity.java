package com.androidapp.attendencecheckqrcode.ui.login;

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
import com.androidapp.attendencecheckqrcode.api.ApiClient;
import com.androidapp.attendencecheckqrcode.models.payloads.AuthResponse;
import com.androidapp.attendencecheckqrcode.models.payloads.LoginRequest;
import com.androidapp.attendencecheckqrcode.ui.home.HomeActivity;
import com.androidapp.attendencecheckqrcode.ui.signup.SignUpActivity;
import com.androidapp.attendencecheckqrcode.utils.TokenManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
        if (tokenManager.getToken() != null) {
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
                    // Sua lai
                    /*
                    if (response.data != null && response.data.isSuccess()) {
                        tokenManager.saveToken(response.data.getToken());
                        saveCredentials(etEmail.getText().toString().trim(), etPassword.getText().toString().trim());
                        Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                        goToHome();
                    } else {
                        Toast.makeText(this, response.data != null ? response.data.getMessage() : "Lỗi server", Toast.LENGTH_SHORT).show();
                    }
                    break;
                     */
                    if (response.data != null && response.data.getAccessToken() != null) {
                        // Lưu token (Dùng getAccessToken thay vì getToken)
                        tokenManager.saveToken(response.data.getAccessToken());
                        saveCredentials(etEmail.getText().toString().trim(), etPassword.getText().toString().trim());

                        Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                        goToHome();
                    } else {
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

        // Gọi hàm xử lý quên mật khẩu
        tvForgotPassword.setOnClickListener(v -> handleForgotPassword());

        btnLogin.setOnClickListener(v -> handleLogin());
    }

    // --- TÍNH NĂNG 1: REMEMBER ME ---
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
            editor.clear(); // Xóa nếu người dùng bỏ tích
        }
        editor.apply();
    }

    // --- TÍNH NĂNG 2: FORGOT PASSWORD ---
    private void handleForgotPassword() {
        // Tạo một Dialog để người dùng nhập Email
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Khôi phục mật khẩu");
        builder.setMessage("Vui lòng nhập Email của bạn. Chúng tôi sẽ gửi hướng dẫn đặt lại mật khẩu.");

        // Khởi tạo một EditText lập trình bằng code để đưa vào Dialog
        final EditText inputEmail = new EditText(this);
        inputEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        inputEmail.setHint("Email đăng ký");

        // Lấy email hiện tại trên màn hình (nếu có) điền sẵn cho tiện
        inputEmail.setText(etEmail.getText().toString().trim());

        builder.setView(inputEmail);

        builder.setPositiveButton("Gửi", (dialog, which) -> {
            String resetEmail = inputEmail.getText().toString().trim();
            if (resetEmail.isEmpty()) {
                Toast.makeText(this, "Email không được để trống!", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "Đang gửi yêu cầu cho: " + resetEmail, Toast.LENGTH_SHORT).show();

            // [DỰ TRÙ API CHO BACKEND]

            /*ApiClient.getApiService(this).forgotPassword(resetEmail).enqueue(new Callback<ApiResponse>() {
                @Override
                public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Đã gửi mã xác nhận qua Email!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(LoginActivity.this, "Email không tồn tại trên hệ thống", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<ApiResponse> call, Throwable t) {
                    Toast.makeText(LoginActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                }
            });*/

        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // --- XỬ LÝ ĐĂNG NHẬP CHÍNH ---
    private void handleLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập Email và Mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Đang đăng nhập...");

        // gọi qua ViewModel
        authViewModel.login(new LoginRequest(email, password));

        //LoginRequest request = new LoginRequest(email, password);
        /*
        ApiClient.getApiService(this).loginUser(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Đăng nhập");

                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().isSuccess()) {

                        // 1. Lưu Token
                        tokenManager.saveToken(response.body().getToken());

                        // 2. Kiểm tra và lưu trạng thái Remember Me
                        saveCredentials(email, password);

                        Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                        goToHome();
                    } else {
                        Toast.makeText(LoginActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    //Toast.makeText(LoginActivity.this, "Sai tài khoản hoặc máy chủ lỗi!", Toast.LENGTH_SHORT).show();

                    int statusCode = response.code();

                    if (statusCode == 400 || statusCode == 401 || statusCode == 404) {
                        Toast.makeText(LoginActivity.this, "Sai Email hoặc Mật khẩu!" + statusCode, Toast.LENGTH_LONG).show();
                    } else if (statusCode >= 500) {
                        Toast.makeText(LoginActivity.this, "Máy chủ đang bảo trì (Lỗi " + statusCode + "). Vui lòng thử lại sau!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(LoginActivity.this, "Đăng nhập thất bại (Mã lỗi: " + statusCode + ")", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Đăng nhập");
                Toast.makeText(LoginActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        */
    }

    private void goToHome() {
        startActivity(new Intent(LoginActivity.this, HomeActivity.class));
        finish();
    }
}