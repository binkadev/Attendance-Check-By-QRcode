package com.androidapp.attendencecheckqrcode.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenManager {
    private SharedPreferences prefs;

    public TokenManager(Context context) {
        // Khởi tạo SharedPreferences với tên file là "AppPrefs"
        prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
    }

    // 1. Lưu JWT Token
    public void saveToken(String token) {
        prefs.edit().putString("JWT_TOKEN", token).apply();
    }

    // 2. Lấy JWT Token
    public String getToken() {
        return prefs.getString("JWT_TOKEN", null);
    }

    // 3. Lưu thông tin User (Tên và Email)
    public void saveUserData(String name, String email) {
        android.util.Log.d("DEBUG_LOGIN", "Đang ghi vào SharedPreferences: " + name);
        prefs.edit()
                .putString("USER_NAME", name)
                .putString("USER_EMAIL", email)
                .apply();
    }

    // 4. Lấy Tên User
    public String getUserName() {
        return prefs.getString("USER_NAME", "Khách");
    }

    // 5. Hàm Xóa sạch dữ liệu (Dùng khi Logout)
    public void clearAll() {
        // Xóa cả Token và thông tin User để App quay về trạng thái chưa đăng nhập
        prefs.edit()
                .remove("JWT_TOKEN")
                .remove("USER_NAME")
                .remove("USER_EMAIL")
                .apply();
    }
}