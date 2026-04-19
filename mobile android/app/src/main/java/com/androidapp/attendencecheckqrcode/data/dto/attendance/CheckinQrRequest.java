package com.androidapp.attendencecheckqrcode.data.dto.attendance;

import com.google.gson.annotations.SerializedName;

public class CheckinQrRequest {
    @SerializedName("token")
    private String token;

    @SerializedName("deviceId")
    private String deviceId;

    @SerializedName("geoLat")
    private double geoLat;

    @SerializedName("geoLng")
    private double geoLng;

    public CheckinQrRequest(String token, String deviceId, double geoLat, double geoLng) {
        this.token = token;
        this.deviceId = deviceId;
        this.geoLat = geoLat;
        this.geoLng = geoLng;
    }
}