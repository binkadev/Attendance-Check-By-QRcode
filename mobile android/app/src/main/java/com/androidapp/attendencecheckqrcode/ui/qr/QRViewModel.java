package com.androidapp.attendencecheckqrcode.ui.qr;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.androidapp.attendencecheckqrcode.data.api.ApiClient;
import com.androidapp.attendencecheckqrcode.data.api.ApiService;
import com.androidapp.attendencecheckqrcode.data.dto.attendance.RotateQrResponse;
import com.androidapp.attendencecheckqrcode.data.repository.AttendanceRepository;
import com.androidapp.attendencecheckqrcode.utils.Resource;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QRViewModel extends AndroidViewModel {

    // ==========================================
    // PHẦN 1: DÀNH CHO SINH VIÊN (QUÉT QR)
    // ==========================================
    private final AttendanceRepository repository;
    private final MutableLiveData<Resource<Void>> checkinResult = new MutableLiveData<>();

    public LiveData<Resource<Void>> getCheckinResult() {
        return checkinResult;
    }

    public void processQRCode(String sessionId, String qrData) {
        checkinResult.setValue(Resource.loading(null));
        repository.checkinQr(sessionId, qrData, checkinResult);
    }

    // ==========================================
    // PHẦN 2: DÀNH CHO GIẢNG VIÊN (TẠO MÃ QR ĐỘNG)
    // ==========================================
    private final ApiService apiService;
    private final MutableLiveData<Resource<String>> qrTokenResult = new MutableLiveData<>();

    public LiveData<Resource<String>> getQrTokenResult() {
        return qrTokenResult;
    }

    public void fetchNewQrToken(String sessionId) {
        qrTokenResult.setValue(Resource.loading(null));
        apiService.rotateQrCode(sessionId).enqueue(new Callback<RotateQrResponse>() {
            @Override
            public void onResponse(Call<RotateQrResponse> call, Response<RotateQrResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    qrTokenResult.setValue(Resource.success(response.body().getToken()));
                } else {
                    qrTokenResult.setValue(Resource.error("Lỗi lấy mã QR từ server", null));
                }
            }

            @Override
            public void onFailure(Call<RotateQrResponse> call, Throwable t) {
                qrTokenResult.setValue(Resource.error("Mất kết nối mạng", null));
            }
        });
    }

    // ==========================================
    // CONSTRUCTOR (Khởi tạo)
    // ==========================================
    public QRViewModel(@NonNull Application application) {
        super(application);
        repository = new AttendanceRepository(application);
        apiService = ApiClient.getApiService(application);
    }
}