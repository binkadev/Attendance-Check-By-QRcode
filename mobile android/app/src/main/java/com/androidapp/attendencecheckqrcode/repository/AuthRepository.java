package com.androidapp.attendencecheckqrcode.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.androidapp.attendencecheckqrcode.api.ApiClient;
import com.androidapp.attendencecheckqrcode.api.ApiService;
import com.androidapp.attendencecheckqrcode.models.payloads.AuthResponse;
import com.androidapp.attendencecheckqrcode.models.payloads.LoginRequest;
import com.androidapp.attendencecheckqrcode.models.payloads.RegisterRequest;
import com.androidapp.attendencecheckqrcode.utils.Resource;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {
    private final ApiService apiService;

    public AuthRepository(Context context) {
        apiService = ApiClient.getApiService(context);
    }

    public LiveData<Resource<AuthResponse>> login(LoginRequest request) {
        MutableLiveData<Resource<AuthResponse>> data = new MutableLiveData<>();
        data.setValue(Resource.loading(null));

        apiService.loginUser(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    data.setValue(Resource.success(response.body()));
                } else {
                    int code = response.code();
                    String errorMsg = (code == 400 || code == 401 || code == 404) ?
                            "Sai Email hoặc Mật khẩu!" + code : "Lỗi máy chủ: " + code;
                    data.setValue(Resource.error(errorMsg, null));
                }
            }
            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                data.setValue(Resource.error("Lỗi kết nối: " + t.getMessage(), null));
            }
        });
        return data;
    }

    public LiveData<Resource<AuthResponse>> register(RegisterRequest request) {
        MutableLiveData<Resource<AuthResponse>> data = new MutableLiveData<>();
        data.setValue(Resource.loading(null));

        apiService.registerUser(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    data.setValue(Resource.success(response.body()));
                } else {
                    //data.setValue(Resource.error("Lỗi từ máy chủ: " + response.code(), null));
                    String errorMsg = "Lỗi " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            // Đọc chuỗi lỗi từ Backend
                            errorMsg = response.errorBody().string();

                            // --- LỆNH IN RA TERMINAL (LOGCAT) ---
                            android.util.Log.e("API_ERROR", "CHI TIẾT LỖI TỪ BACKEND: " + errorMsg);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // Chỉ báo lên UI câu ngắn gọn thôi
                    data.setValue(Resource.error("Đăng ký thất bại. Đang in lỗi ra Logcat! - " + response.code(), null));
                }
            }
            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                data.setValue(Resource.error("Lỗi mạng: " + t.getMessage(), null));
            }
        });
        return data;
    }
}