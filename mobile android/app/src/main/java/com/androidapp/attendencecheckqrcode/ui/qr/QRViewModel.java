package com.androidapp.attendencecheckqrcode.ui.qr;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel; // 1. Đảm bảo import đúng
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.androidapp.attendencecheckqrcode.repository.AttendanceRepository;
import com.androidapp.attendencecheckqrcode.utils.Resource;

// 2. Bắt buộc phải có "extends AndroidViewModel" ở dòng này
public class QRViewModel extends AndroidViewModel {
    private final AttendanceRepository repository;
    private final MutableLiveData<Resource<Void>> checkinResult = new MutableLiveData<>();

    public QRViewModel(@NonNull Application application) {
        super(application); // 3. Bắt buộc gọi super()
        repository = new AttendanceRepository(application);
    }

    public LiveData<Resource<Void>> getCheckinResult() {
        return checkinResult;
    }

    public void processQRCode(int sessionId, String qrData) {
        repository.checkinQr(sessionId, qrData).observeForever(checkinResult::setValue);
    }
}
