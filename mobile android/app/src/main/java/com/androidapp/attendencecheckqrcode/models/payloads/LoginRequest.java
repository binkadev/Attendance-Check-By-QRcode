package com.androidapp.attendencecheckqrcode.models.payloads;

public class LoginRequest {
    private String email;
    private String password;
    private String deviceId; // Thêm trường này

    public LoginRequest(String email, String password, String deviceId) {
        this.email = email;
        this.password = password;
        this.deviceId = deviceId;
    }
}
