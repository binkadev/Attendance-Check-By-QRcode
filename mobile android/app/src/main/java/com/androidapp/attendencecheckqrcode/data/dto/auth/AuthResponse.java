package com.androidapp.attendencecheckqrcode.data.dto.auth;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {
    @SerializedName("tokenType")
    private String tokenType;

    @SerializedName("accessToken")
    private String accessToken;

    @SerializedName("accessTokenExpiresAt")
    private String accessTokenExpiresAt;

    @SerializedName("refreshToken")
    private String refreshToken;

    @SerializedName("refreshTokenExpiresAt")
    private String refreshTokenExpiresAt;

    @SerializedName("sessionId")
    private String sessionId;

    @SerializedName("firstLogin")
    private boolean firstLogin;

    @SerializedName("user")
    private UserDto user;

    public String getAccessToken() { return accessToken; }
    public String getTokenType() { return tokenType; }
    public String getRefreshToken() { return refreshToken; }
    public String getAccessTokenExpiresAt() { return accessTokenExpiresAt; }
    public String getRefreshTokenExpiresAt() { return refreshTokenExpiresAt; }
    public String getSessionId() { return sessionId; }
    public boolean isFirstLogin() { return firstLogin; }
    public UserDto getUser() { return user; }

    public static class UserDto {
        @SerializedName("id")
        private String id;

        @SerializedName("email")
        private String email;

        @SerializedName("fullName")
        private String fullName;

        @SerializedName("platformRole")
        private String platformRole;

        public String getId() { return id; }
        public String getEmail() { return email; }
        public String getFullName() { return fullName; }
        public String getPlatformRole() { return platformRole; }
    }
}