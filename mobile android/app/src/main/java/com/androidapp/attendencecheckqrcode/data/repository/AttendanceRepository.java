package com.androidapp.attendencecheckqrcode.data.repository;

import android.content.Context;
import android.provider.Settings;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.androidapp.attendencecheckqrcode.data.api.ApiClient;
import com.androidapp.attendencecheckqrcode.data.api.ApiService;
import com.androidapp.attendencecheckqrcode.data.dto.attendance.CheckinQrRequest;
import com.androidapp.attendencecheckqrcode.utils.Resource;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AttendanceRepository {
    private final ApiService apiService;
    private final Context context;

    public AttendanceRepository(Context context) {
        this.context = context;
        apiService = ApiClient.getApiService(context);
    }

    // Đã đổi int thành String sessionId
    public LiveData<Resource<Void>> checkinQr(String sessionId, String qrCodeData) {
        MutableLiveData<Resource<Void>> data = new MutableLiveData<>();
        data.setValue(Resource.loading(null));

        // 1. Lấy Device ID để Backend chống gian lận đa thiết bị
        String deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        // 2. Đóng gói dữ liệu (Tọa độ tạm thời truyền 0.0 nếu chưa làm tính năng GPS)
        CheckinQrRequest request = new CheckinQrRequest(qrCodeData, deviceId, 0.0, 0.0);

        // 3. Gửi lên Server (Truyền cả sessionId và request body)
        apiService.checkinQr(sessionId, request).enqueue(new Callback<Void>() {
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