package com.androidapp.attendencecheckqrcode.repository;


import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.androidapp.attendencecheckqrcode.api.ApiClient;
import com.androidapp.attendencecheckqrcode.api.ApiService;
import com.androidapp.attendencecheckqrcode.utils.Resource;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AttendanceRepository {
    private final ApiService apiService;

    // Đây chính là constructor mà QRViewModel đang tìm kiếm
    public AttendanceRepository(Context context) {
        apiService = ApiClient.getApiService(context);
    }

    public LiveData<Resource<Void>> checkinQr(int sessionId, String qrCodeData) {
        MutableLiveData<Resource<Void>> data = new MutableLiveData<>();
        data.setValue(Resource.loading(null));

        apiService.checkinQr(sessionId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    data.setValue(Resource.success(null));
                } else {
                    int code = response.code();
                    String errorMsg = (code == 400) ? "Mã QR không hợp lệ hoặc đã hết hạn" :
                            (code == 409) ? "Bạn đã điểm danh rồi" :
                                    "Lỗi điểm danh: " + code;
                    data.setValue(Resource.error(errorMsg, null));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                data.setValue(Resource.error("Lỗi kết nối mạng: " + t.getMessage(), null));
            }
        });
        return data;
    }
}