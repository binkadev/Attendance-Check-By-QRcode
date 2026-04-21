package com.androidapp.attendencecheckqrcode.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenManager {
    private SharedPreferences prefs;

    public TokenManager(Context context) {
        prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
    }

    public void saveTokens(String accessToken, String refreshToken) {
        prefs.edit()
                .putString("ACCESS_TOKEN", accessToken)
                .putString("REFRESH_TOKEN", refreshToken)
                .apply();
    }

    public String getAccessToken() {
        return prefs.getString("ACCESS_TOKEN", null);
    }

    public String getRefreshToken() {
        return prefs.getString("REFRESH_TOKEN", null);
    }

    public void saveUserData(String name, String email) {
        android.util.Log.d("DEBUG_LOGIN", "Đang ghi vào SharedPreferences: " + name);
        prefs.edit()
                .putString("USER_NAME", name)
                .putString("USER_EMAIL", email)
                .apply();
    }

    public String getUserName() {
        return prefs.getString("USER_NAME", "Khách");
    }

    public void clearAll() {
        prefs.edit()
                .remove("ACCESS_TOKEN")
                .remove("REFRESH_TOKEN")
                .remove("USER_NAME")
                .remove("USER_EMAIL")
                .apply();
    }
}