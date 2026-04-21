package com.androidapp.attendencecheckqrcode.ui.settings;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.androidapp.attendencecheckqrcode.data.api.ApiService;
import com.androidapp.attendencecheckqrcode.data.dto.auth.AuthResponse;
import com.androidapp.attendencecheckqrcode.data.dto.auth.ChangePasswordRequest;
import com.androidapp.attendencecheckqrcode.domain.models.User;
import com.androidapp.attendencecheckqrcode.utils.TokenManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsViewModel extends ViewModel {

    // ==========================================
    // 1. CÁC BIẾN LIVEDATA (ACTIVITY SẼ LẮNG NGHE CÁC BIẾN NÀY)
    // ==========================================
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private final MutableLiveData<User> userProfile = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoggedOut = new MutableLiveData<>(false);

    // Getters cho Activity
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getToastMessage() { return toastMessage; }
    public LiveData<User> getUserProfile() { return userProfile; }
    public LiveData<Boolean> getIsLoggedOut() { return isLoggedOut; }


    // ==========================================
    // 2. GỌI API: LẤY THÔNG TIN CÁ NHÂN (PROFILE)
    // ==========================================
    public void fetchUserProfile(ApiService apiService) {
        isLoading.setValue(true);

        apiService.getMyProfile().enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    // Bắn dữ liệu User ra cho Activity tự động cập nhật UI
                    userProfile.setValue(response.body());
                } else {
                    int errorCode = response.code();
                    android.util.Log.e("DEBUG_API", "===> Lỗi lấy Profile, Mã Code: " + errorCode);
                    toastMessage.setValue("Lỗi lấy thông tin (Code " + errorCode + ")");
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                isLoading.setValue(false);
                android.util.Log.e("DEBUG_API", "===> Lỗi mạng / Lỗi parse JSON: " + t.getMessage());
                toastMessage.setValue("Lỗi kết nối: " + t.getMessage());
            }
        });
    }


    // ==========================================
    // 3. GỌI API: ĐỔI MẬT KHẨU
    // ==========================================
    public void changePassword(ApiService apiService, String oldPassword, String newPassword) {
        isLoading.setValue(true);

        ChangePasswordRequest request = new ChangePasswordRequest(oldPassword, newPassword);

        apiService.changePassword(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                isLoading.setValue(false);

                if (response.isSuccessful()) {
                    toastMessage.setValue("Đổi mật khẩu thành công!");
                } else {
                    try {
                        String errorDetails = response.errorBody().string();
                        android.util.Log.e("DEBUG_API", "===> LÝ DO BACKEND TỪ CHỐI: " + errorDetails);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    toastMessage.setValue("Mật khẩu cũ không đúng hoặc lỗi dữ liệu!");
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                isLoading.setValue(false);
                toastMessage.setValue("Lỗi kết nối mạng, vui lòng thử lại!");
            }
        });
    }


    // ==========================================
    // 4. LOGIC: ĐĂNG XUẤT
    // ==========================================
    public void logout(TokenManager tokenManager) {
        // Xóa sạch Token và dữ liệu đã lưu
        tokenManager.clearAll();
        // Bắn tín hiệu để Activity biết đường nhảy về trang Login
        isLoggedOut.setValue(true);
    }
}