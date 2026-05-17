package com.ptithcm.attendapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SignInViewModel extends ViewModel {

    // Lớp nội bộ để bọc trạng thái đăng nhập
    public static class LoginState {
        public boolean isLoading;
        public boolean isSuccess;
        public String errorMessage;

        public LoginState(boolean isLoading, boolean isSuccess, String errorMessage) {
            this.isLoading = isLoading;
            this.isSuccess = isSuccess;
            this.errorMessage = errorMessage;
        }
    }

    private final MutableLiveData<LoginState> loginState = new MutableLiveData<>();

    public LiveData<LoginState> getLoginState() {
        return loginState;
    }

    public void loginWithEmail(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            loginState.setValue(new LoginState(false, false, "Vui lòng nhập đầy đủ thông tin!"));
            return;
        }

        // Bắt đầu loading
        loginState.setValue(new LoginState(true, false, null));

        // Mock delay gọi API (Sau này thay bằng Retrofit/Volley gọi lên Spring Boot)
        new android.os.Handler().postDelayed(() -> {
            if (email.equals("1") && password.equals("1")) {
                loginState.setValue(new LoginState(false, true, null));
            } else {
                loginState.setValue(new LoginState(false, false, "Sai tài khoản hoặc mật khẩu!"));
            }
        }, 1500);
    }
}