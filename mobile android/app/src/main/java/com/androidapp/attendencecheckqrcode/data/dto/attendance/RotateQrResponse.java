package com.androidapp.attendencecheckqrcode.data.dto.attendance;

import com.google.gson.annotations.SerializedName;

public class RotateQrResponse {
    @SerializedName("sessionId")
    private String sessionId;

    @SerializedName("token")
    private String token;

    @SerializedName("issuedAt")
    private String issuedAt;

    @SerializedName("expiresAt")
    private String expiresAt;

    public String getToken() {
        return token;
    }
}