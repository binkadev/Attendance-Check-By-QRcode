package com.androidapp.attendencecheckqrcode.ui.home;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.androidapp.attendencecheckqrcode.utils.TokenManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HomeViewModel extends AndroidViewModel {

    private final TokenManager tokenManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable timeUpdater;

    // Các LiveData để UI (Activity) quan sát và tự động cập nhật
    private final MutableLiveData<String> greetingText = new MutableLiveData<>();
    private final MutableLiveData<String> currentDateText = new MutableLiveData<>();
    private final MutableLiveData<String> userName = new MutableLiveData<>();

    public HomeViewModel(@NonNull Application application) {
        super(application);
        tokenManager = new TokenManager(application);

        // Vừa khởi tạo là lấy dữ liệu ngay
        loadUserData();
        startUpdatingTime();
    }

    // --- GETTERS ---
    public LiveData<String> getGreetingText() { return greetingText; }
    public LiveData<String> getCurrentDateText() { return currentDateText; }
    public LiveData<String> getUserName() { return userName; }

    // --- LOGIC XỬ LÝ ---
    public void loadUserData() {
        // Lấy tên từ SharedPreferences
        String name = tokenManager.getUserName();
        userName.setValue(name);
    }

    private void updateTimeAndGreeting() {
        // 1. Xử lý Lời chào
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (hour >= 6 && hour < 12) {
            greetingText.setValue("Chào buổi sáng 🌤️");
        } else if (hour >= 12 && hour < 18) {
            greetingText.setValue("Chào buổi chiều ☀️");
        } else {
            greetingText.setValue("Chào buổi tối 🌙");
        }

        // 2. Xử lý Ngày tháng
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE dd/MM - HH:mm", new Locale("vi", "VN"));
        String dateString = "📅 HÔM NAY, " + sdf.format(new Date()).toUpperCase();
        currentDateText.setValue(dateString);
    }

    private void startUpdatingTime() {
        timeUpdater = new Runnable() {
            @Override
            public void run() {
                updateTimeAndGreeting();
                // Lặp lại sau mỗi 60 giây
                handler.postDelayed(this, 60000);
            }
        };
        handler.post(timeUpdater);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Chống rò rỉ bộ nhớ (Memory Leak) khi thoát App
        if (handler != null && timeUpdater != null) {
            handler.removeCallbacks(timeUpdater);
        }
    }
}