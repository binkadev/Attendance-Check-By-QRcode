package com.androidapp.attendencecheckqrcode.data.dto.auth;

public class LoginRequest {
    private String email;
    private String password;
    private String deviceId;

    public LoginRequest(String email, String password, String deviceId) {
        this.email = email;
        this.password = password;
        this.deviceId = deviceId;
    }
}
