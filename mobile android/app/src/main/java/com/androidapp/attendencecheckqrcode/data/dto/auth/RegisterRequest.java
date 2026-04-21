package com.androidapp.attendencecheckqrcode.data.dto.auth;

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


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}