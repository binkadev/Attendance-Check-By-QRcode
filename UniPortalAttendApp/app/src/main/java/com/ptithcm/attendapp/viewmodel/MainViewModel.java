package com.ptithcm.attendapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MainViewModel extends ViewModel {

    private final MutableLiveData<String> userName = new MutableLiveData<>();
    private final MutableLiveData<Integer> presentCount = new MutableLiveData<>();

    public MainViewModel() {
        // Tạm thời mock dữ liệu. Sau này sẽ lấy từ Database/API trả về
        userName.setValue("Phạm Văn Phú");
        presentCount.setValue(42);
    }

    public LiveData<String> getUserName() {
        return userName;
    }

    public void processQRCode(String qrData) {
        // Xử lý chuỗi dữ liệu quét được từ thiết bị Camera hoặc thiết bị giả lập
    }
}