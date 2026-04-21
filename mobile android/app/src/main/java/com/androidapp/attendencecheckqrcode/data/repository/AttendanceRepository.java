package com.androidapp.attendencecheckqrcode.data.repository;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;

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
        this.context = context.getApplicationContext(); // Dùng ApplicationContext cho an toàn
        apiService = ApiClient.getApiService(context);
    }

    /**
     * HÀM ĐIỂM DANH TỔNG HỢP (Hợp nhất logic Anti-Fraud và Architecture mới)
     * @param sessionId ID của phiên điểm danh (UUID)
     * @param qrData Chuỗi raw quét từ QR (Gồm groupId_timestamp_secret)
     * @param resultLiveData Cầu nối để báo kết quả về UI
     */
    public void checkinQr(String sessionId, String qrData, MutableLiveData<Resource<Void>> resultLiveData) {

        @SuppressLint("HardwareIds")
        String deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        CheckinQrRequest request = new CheckinQrRequest(qrData, deviceId, 0.0, 0.0);

        apiService.checkinQr(sessionId, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("ATTENDANCE_REPO", "Điểm danh thành công cho session: " + sessionId);
                    resultLiveData.setValue(Resource.success(null));
                } else {
                    int code = response.code();
                    String errorMsg;
                    switch (code) {
                        case 400: errorMsg = "Mã QR không hợp lệ hoặc đã hết hạn"; break;
                        case 409: errorMsg = "Bạn đã điểm danh cho lớp này rồi"; break;
                        case 403: errorMsg = "Thiết bị không hợp lệ hoặc bị từ chối"; break;
                        default: errorMsg = "Lỗi hệ thống: " + code; break;
                    }

                    try {
                        if (response.errorBody() != null) {
                            Log.e("ATTENDANCE_REPO", "Server Error Body: " + response.errorBody().string());
                        }
                    } catch (Exception ignored) {}

                    resultLiveData.setValue(Resource.error(errorMsg, null));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("ATTENDANCE_REPO", "Lỗi kết nối mạng: " + t.getMessage());
                resultLiveData.setValue(Resource.error("Không thể kết nối đến máy chủ!", null));
            }
        });
    }
}