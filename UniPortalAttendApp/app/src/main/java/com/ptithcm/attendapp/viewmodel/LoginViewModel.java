package com.ptithcm.attendapp.viewmodel;

// Đã thêm dòng import quan trọng này
import androidx.lifecycle.ViewModel;

public class LoginViewModel extends ViewModel {
    // Nơi chứa các logic gọi API, kiểm tra Device ID hoặc xử lý SSO

    public void performSSOLogin() {
        // Logic đăng nhập qua tài khoản trường
    }

    public void performStandardLogin() {
        // Logic đăng nhập Email
    }

    public void performBiometricAuth() {
        // Gọi API sinh trắc học
    }
}