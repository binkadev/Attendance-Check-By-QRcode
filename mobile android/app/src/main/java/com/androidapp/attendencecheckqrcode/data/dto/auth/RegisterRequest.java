package com.androidapp.attendencecheckqrcode.data.dto.auth;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {

    @SerializedName("email")
    private String email;

    @SerializedName("password")
    private String password;

    @SerializedName("fullName")
    private String fullName;

    @SerializedName("userCode")
    private String userCode;

    @SerializedName("deviceId")
    private String deviceId;

    // Bắt buộc constructor phải có đủ 5 tham số theo đúng thứ tự
    public RegisterRequest(String email, String password, String fullName, String userCode, String deviceId) {
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.userCode = userCode;
        this.deviceId = deviceId;
    }
}