package com.ptithcm.attendapp.model;

public class AuthResponse {
    private String tokenType;
    private String accessToken;
    private String refreshToken;
    private User user;

    // Getters
    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public User getUser() { return user; }

    // Class con chứa thông tin User
    public static class User {
        private String id;
        private String email;
        private String fullName;
        private String platformRole;

        public String getFullName() { return fullName; }
        public String getEmail() { return email; }
    }
}