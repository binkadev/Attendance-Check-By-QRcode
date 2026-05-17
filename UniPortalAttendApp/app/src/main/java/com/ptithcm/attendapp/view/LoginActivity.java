package com.ptithcm.attendapp.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

// Đã sửa lại đúng package của project
import com.ptithcm.attendapp.R;
import com.ptithcm.attendapp.viewmodel.LoginViewModel;

public class LoginActivity extends AppCompatActivity {

    private LoginViewModel loginViewModel;

    // Khai báo Views
    private MaterialCardView cardUniversityLogin, cardEmailLogin, cardBiometric;
    private MaterialButton btnContinue;
    private int selectedMethod = 0; // 1: SSO, 2: Email

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Khởi tạo ViewModel
        loginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        initViews();
        setupListeners();
    }

    private void initViews() {
        cardUniversityLogin = findViewById(R.id.cardUniversityLogin);
        cardEmailLogin = findViewById(R.id.cardEmailLogin);
        cardBiometric = findViewById(R.id.cardBiometric);
        btnContinue = findViewById(R.id.btnContinue);
    }

    private void setupListeners() {
        cardUniversityLogin.setOnClickListener(v -> {
            selectedMethod = 1;
            highlightSelection(cardUniversityLogin);
        });

        cardEmailLogin.setOnClickListener(v -> {
            selectedMethod = 2;
            highlightSelection(cardEmailLogin);
        });

        cardBiometric.setOnClickListener(v -> {
            loginViewModel.performBiometricAuth();
            Toast.makeText(this, "Đang khởi động Sinh trắc học...", Toast.LENGTH_SHORT).show();
        });

        btnContinue.setOnClickListener(v -> {
            if (selectedMethod == 1) {
                loginViewModel.performSSOLogin();
                Toast.makeText(this, "Chuyển hướng đăng nhập SSO...", Toast.LENGTH_SHORT).show();
            } else if (selectedMethod == 2) {
                Intent intent = new Intent(LoginActivity.this, SignInActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Vui lòng chọn một phương thức!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void highlightSelection(MaterialCardView selectedCard) {
        // Reset giao diện các card
        cardUniversityLogin.setStrokeWidth(0);
        cardEmailLogin.setStrokeWidth(0);

        // Highlight card được chọn bằng viền màu đỏ
        selectedCard.setStrokeWidth(3);
        // Đã sửa lại cách gọi màu chuẩn xác cho các phiên bản Android mới
        selectedCard.setStrokeColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
    }
}