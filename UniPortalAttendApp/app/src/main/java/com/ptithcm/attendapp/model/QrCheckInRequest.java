package com.ptithcm.attendapp.model;

import com.google.gson.annotations.SerializedName;

public class QrCheckInRequest {

    // 🚨 QUAN TRỌNG: Server báo "token is required" nên phải map đúng tên này
    @SerializedName("token")
    private String qrTokenId;

    // Kiểm tra lại với Backend xem các field này có đúng tên trong JSON không
    // Nếu Backend để camelCase (deviceId) thì giữ nguyên,
    // nếu Backend dùng snake_case (device_id) thì phải thêm @SerializedName vào.
    private String deviceId;
    private Double geoLat;
    private Double geoLng;

    // Constructor tối thiểu
    public QrCheckInRequest(String qrTokenId) {
        this.qrTokenId = qrTokenId;
    }

    // Constructor đầy đủ để gửi kèm GPS và Device ID
    public QrCheckInRequest(String qrTokenId, String deviceId, Double geoLat, Double geoLng) {
        this.qrTokenId = qrTokenId;
        this.deviceId = deviceId;
        this.geoLat = geoLat;
        this.geoLng = geoLng;
    }

    // Getter và Setter giữ nguyên...
    public String getQrTokenId() { return qrTokenId; }
    public void setQrTokenId(String qrTokenId) { this.qrTokenId = qrTokenId; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public Double getGeoLat() { return geoLat; }
    public void setGeoLat(Double geoLat) { this.geoLat = geoLat; }

    public Double getGeoLng() { return geoLng; }
    public void setGeoLng(Double geoLng) { this.geoLng = geoLng; }

    @Override
    public String toString() {
        return "QrCheckInRequest{" +
                "token='" + qrTokenId + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", geoLat=" + geoLat +
                ", geoLng=" + geoLng +
                '}';
    }
}