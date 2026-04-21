package com.androidapp.attendencecheckqrcode.data.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.androidapp.attendencecheckqrcode.data.api.ApiClient;
import com.androidapp.attendencecheckqrcode.data.api.ApiService;
import com.androidapp.attendencecheckqrcode.data.dto.auth.AuthResponse;
import com.androidapp.attendencecheckqrcode.data.dto.auth.LoginRequest;
import com.androidapp.attendencecheckqrcode.data.dto.auth.RegisterRequest;
import com.androidapp.attendencecheckqrcode.domain.models.User;
import com.androidapp.attendencecheckqrcode.utils.Resource;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {
    private final ApiService apiService;

    public AuthRepository(Context context) {
        apiService = ApiClient.getApiService(context);
    }

    // 1. ĐĂNG NHẬP
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
                            "Sai Email hoặc Mật khẩu! (" + code + ")" : "Lỗi máy chủ: " + code;
                    data.setValue(Resource.error(errorMsg, null));
                }
            }
            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                data.setValue(Resource.error("Lỗi kết nối mạng: " + t.getMessage(), null));
            }
        });
        return data;
    }

    // 2. ĐĂNG KÝ
    public LiveData<Resource<AuthResponse>> register(RegisterRequest request) {
        MutableLiveData<Resource<AuthResponse>> data = new MutableLiveData<>();
        data.setValue(Resource.loading(null));

        apiService.registerUser(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    data.setValue(Resource.success(response.body()));
                } else {
                    String errorMsg = "Lỗi " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                            android.util.Log.e("API_ERROR", "Lỗi Backend: " + errorMsg);
                        }
                    } catch (Exception e) { e.printStackTrace(); }

                    data.setValue(Resource.error("Đăng ký thất bại. Mã lỗi: " + response.code(), null));
                }
            }
            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                data.setValue(Resource.error("Lỗi kết nối mạng: " + t.getMessage(), null));
            }
        });
        return data;
    }
}