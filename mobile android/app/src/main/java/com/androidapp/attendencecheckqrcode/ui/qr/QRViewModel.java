package com.androidapp.attendencecheckqrcode.ui.qr;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.androidapp.attendencecheckqrcode.data.repository.AttendanceRepository;
import com.androidapp.attendencecheckqrcode.utils.Resource;

public class QRViewModel extends AndroidViewModel {
    private final AttendanceRepository repository;
    private final MutableLiveData<Resource<Void>> checkinResult = new MutableLiveData<>();

    public QRViewModel(@NonNull Application application) {
        super(application);
        repository = new AttendanceRepository(application);
    }

    public LiveData<Resource<Void>> getCheckinResult() {
        return checkinResult;
    }

    public void processQRCode(String sessionId, String qrData) {
        checkinResult.setValue(Resource.loading(null));
        repository.checkinQr(sessionId, qrData, checkinResult);
    }
}