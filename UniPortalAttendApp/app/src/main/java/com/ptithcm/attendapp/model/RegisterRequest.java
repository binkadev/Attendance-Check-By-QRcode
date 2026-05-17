package com.ptithcm.attendapp.model;

public class RegisterRequest {
    private String email;
    private String password;
    private String fullName;
    private String userCode;
    private String deviceId;

    public RegisterRequest(String email, String password, String fullName, String userCode, String deviceId) {
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.userCode = userCode;
        this.deviceId = deviceId;
    }
}