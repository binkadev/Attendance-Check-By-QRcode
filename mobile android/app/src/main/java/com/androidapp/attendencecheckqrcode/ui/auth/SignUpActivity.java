package com.androidapp.attendencecheckqrcode.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.androidapp.attendencecheckqrcode.R;
import com.androidapp.attendencecheckqrcode.data.dto.auth.RegisterRequest;

import java.util.Calendar;

public class SignUpActivity extends AppCompatActivity {

    private ImageView btnBack, btnShowPass;
    private TextView tvLoginLink;
    private Button btnSignUp;
    private EditText etFirstName, etLastName, etEmail, etPassword;

    private boolean isPasswordVisible = false;

    private EditText etConfirmPassword;
    private ImageView btnShowConfirmPass;
    private boolean isConfirmPasswordVisible = false;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        initViews();
        setupListeners();
        observeViewModel();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvLoginLink = findViewById(R.id.tvLoginLink);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnShowPass = findViewById(R.id.btnShowPass);
        btnSignUp = findViewById(R.id.btnSignUp);

        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnShowConfirmPass = findViewById(R.id.btnShowConfirmPass);
    }

    private void observeViewModel() {
        authViewModel.getRegisterResult().observe(this, response -> {
            switch (response.status) {
                case LOADING:
                    btnSignUp.setEnabled(false);
                    btnSignUp.setText("Đang xử lý...");
                    break;
                case SUCCESS:
                    btnSignUp.setEnabled(true);
                    btnSignUp.setText("Đăng ký");

                    /*
                    if (response.data != null && response.data.isSuccess()) {
                        Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, response.data != null ? response.data.getMessage() : "Lỗi server", Toast.LENGTH_LONG).show();
                    }
                    break;
                     */
                    // CHỈ CẦN VÀO ĐƯỢC CASE SUCCESS LÀ ĐÃ THÀNH CÔNG, BỎ isSuccess() VÀ getMessage()
                    Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();

                    break;
                case ERROR:
                    btnSignUp.setEnabled(true);
                    btnSignUp.setText("Đăng ký");
                    Toast.makeText(this, response.message, Toast.LENGTH_LONG).show();
                    break;
            }
        });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        tvLoginLink.setOnClickListener(v -> finish());
//        etDob.setOnClickListener(v -> showDatePicker());

        btnShowPass.setOnClickListener(v -> {
            if (isPasswordVisible) {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            } else {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            }
            etPassword.setSelection(etPassword.getText().length());
            isPasswordVisible = !isPasswordVisible;
        });

        btnShowConfirmPass.setOnClickListener(v -> {
            if (isConfirmPasswordVisible) {
                etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            } else {
                etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            }
            etConfirmPassword.setSelection(etConfirmPassword.getText().length());
            isConfirmPasswordVisible = !isConfirmPasswordVisible;
        });

        btnSignUp.setOnClickListener(v -> handleSignUp());
    }

    private void handleSignUp() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(firstName) || TextUtils.isEmpty(lastName) ||
                TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Vui lòng điền đủ tất cả các trường!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Mật khẩu xác nhận không khớp!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 10) {
            Toast.makeText(this, "Mật khẩu phải có ít nhất 10 ký tự!", Toast.LENGTH_SHORT).show();
            return;
        }

        String passwordPattern = "^(?=.*[A-Za-z])(?=.*\\d).+$";
        if (!password.matches(passwordPattern)) {
            Toast.makeText(this, "Mật khẩu phải bao gồm cả chữ và số!", Toast.LENGTH_SHORT).show();
            return;
        }

        String fullName = firstName + " " + lastName;

        String deviceId = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        String safeUserCode = email.split("@")[0];
        String safeEmail = email.toLowerCase();


        RegisterRequest request = new RegisterRequest(safeEmail, password, fullName, safeUserCode, deviceId);

        String jsonToSend = new com.google.gson.Gson().toJson(request);
        android.util.Log.e("KIEM_TRA_JSON", "CỤC JSON ANDROID GỬI LÊN: " + jsonToSend);

        authViewModel.register(request);
    }

    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
//        new DatePickerDialog(this, (view, y, m, d) -> {
//            etDob.setText(d + "/" + (m + 1) + "/" + y);
//        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }
}