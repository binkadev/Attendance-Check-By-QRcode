package com.androidapp.attendencecheckqrcode.data.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.androidapp.attendencecheckqrcode.data.api.ApiClient;
import com.androidapp.attendencecheckqrcode.data.api.ApiService;
import com.androidapp.attendencecheckqrcode.data.dto.auth.AuthResponse;
import com.androidapp.attendencecheckqrcode.data.dto.auth.LoginRequest;
import com.androidapp.attendencecheckqrcode.data.dto.auth.RegisterRequest;
import com.androidapp.attendencecheckqrcode.utils.Resource;

import org.json.JSONObject;

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
                    // --- ĐỌC CHI TIẾT LỖI TỪ BACKEND ---
                    String errorMsg = "Tài khoản không tồn tại hoặc sai mật khẩu!"; // Mặc định cho 401

                    try {
                        if (response.errorBody() != null) {
                            String errorJson = response.errorBody().string();
                            JSONObject jsonObject = new JSONObject(errorJson);

                            // Lấy chính xác lời nhắn từ Backend gửi về (nếu có key "message")
                            if (jsonObject.has("message")) {
                                String backendMessage = jsonObject.getString("message");

                                // Dịch các lỗi phổ biến sang Tiếng Việt cho User dễ hiểu
                                if (backendMessage.toLowerCase().contains("invalid email or password") ||
                                        backendMessage.toLowerCase().contains("bad credentials")) {
                                    errorMsg = "Tài khoản không tồn tại hoặc sai mật khẩu!";
                                } else if (backendMessage.toLowerCase().contains("user not found")) {
                                    errorMsg = "Tài khoản này chưa được đăng ký!";
                                } else {
                                    errorMsg = backendMessage; // Lấy y nguyên lời Backend báo
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        errorMsg = "Lỗi máy chủ: " + response.code();
                    }

                    data.setValue(Resource.error(errorMsg, null));
                }
            }
            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                data.setValue(Resource.error("Lỗi kết nối mạng: Vui lòng kiểm tra lại Wifi/3G!", null));
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
                    // --- ĐỌC CHI TIẾT LỖI TỪ BACKEND KHI ĐĂNG KÝ ---
                    String errorMsg = "Đăng ký thất bại!";

                    try {
                        if (response.errorBody() != null) {
                            String errorJson = response.errorBody().string();
                            JSONObject jsonObject = new JSONObject(errorJson);

                            if (jsonObject.has("message")) {
                                String backendMessage = jsonObject.getString("message");

                                // Dịch một số lỗi quen thuộc
                                if (backendMessage.toLowerCase().contains("email already exists") ||
                                        backendMessage.toLowerCase().contains("already taken")) {
                                    errorMsg = "Email này đã được sử dụng. Vui lòng chọn Email khác!";
                                } else {
                                    errorMsg = backendMessage;
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        errorMsg = "Lỗi hệ thống: " + response.code();
                    }

                    data.setValue(Resource.error(errorMsg, null));
                }
            }
            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                data.setValue(Resource.error("Lỗi kết nối mạng: Vui lòng kiểm tra lại Wifi/3G!", null));
            }
        });
        return data;
    }
}