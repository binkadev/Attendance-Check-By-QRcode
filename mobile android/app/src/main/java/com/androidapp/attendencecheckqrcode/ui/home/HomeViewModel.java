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

    private final MutableLiveData<String> greetingText = new MutableLiveData<>();
    private final MutableLiveData<String> currentDateText = new MutableLiveData<>();
    private final MutableLiveData<String> userName = new MutableLiveData<>();

    public HomeViewModel(@NonNull Application application) {
        super(application);
        tokenManager = new TokenManager(application);

        loadUserData();
        startUpdatingTime();
    }

    public LiveData<String> getGreetingText() { return greetingText; }
    public LiveData<String> getCurrentDateText() { return currentDateText; }
    public LiveData<String> getUserName() { return userName; }

    public void loadUserData() {
        String name = tokenManager.getUserName();
        userName.setValue(name);
    }

    private void updateTimeAndGreeting() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (hour >= 6 && hour < 12) {
            greetingText.setValue("Chào buổi sáng 🌤️");
        } else if (hour >= 12 && hour < 18) {
            greetingText.setValue("Chào buổi chiều ☀️");
        } else {
            greetingText.setValue("Chào buổi tối 🌙");
        }

        SimpleDateFormat sdf = new SimpleDateFormat("EEEE dd/MM - HH:mm", new Locale("vi", "VN"));
        String dateString = "📅 HÔM NAY, " + sdf.format(new Date()).toUpperCase();
        currentDateText.setValue(dateString);
    }

    private void startUpdatingTime() {
        timeUpdater = new Runnable() {
            @Override
            public void run() {
                updateTimeAndGreeting();
                handler.postDelayed(this, 60000);
            }
        };
        handler.post(timeUpdater);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (handler != null && timeUpdater != null) {
            handler.removeCallbacks(timeUpdater);
        }
    }
}