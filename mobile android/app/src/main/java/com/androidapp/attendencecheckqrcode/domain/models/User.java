package com.androidapp.attendencecheckqrcode.domain.models;

import java.io.Serializable;

public class User implements Serializable {

    private String id;

    private String email;
    private String fullName;
    private String userCode;
    private String phone;

    public User() {
    }

    public User(String id, String email, String fullName, String userCode) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.userCode = userCode;
    }

    // =========================================
    // GETTERS VÀ SETTERS
    // =========================================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}