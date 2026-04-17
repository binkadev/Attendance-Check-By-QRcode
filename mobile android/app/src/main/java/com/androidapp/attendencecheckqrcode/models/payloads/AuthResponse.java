package com.androidapp.attendencecheckqrcode.models.payloads;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {
    @SerializedName("tokenType")
    private String tokenType;

    @SerializedName("accessToken")
    private String accessToken;

    @SerializedName("firstLogin")
    private boolean firstLogin;

    @SerializedName("user")
    private UserDto user;

    // Getters
    public String getAccessToken() { return accessToken; }
    public String getTokenType() { return tokenType; }
    public boolean isFirstLogin() { return firstLogin; }
    public UserDto getUser() { return user; }

    // Class con để hứng object "user" bên trong JSON
    public static class UserDto {
        private String id;
        private String email;
        private String fullName;
        private String platformRole;

        public String getId() { return id; }
        public String getFullName() { return fullName; }
    }
}
