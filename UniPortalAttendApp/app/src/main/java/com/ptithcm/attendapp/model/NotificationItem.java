package com.ptithcm.attendapp.model;
import com.google.gson.annotations.SerializedName;

public class NotificationItem {
    @SerializedName("id") private String id;
    @SerializedName("title") private String title;
    @SerializedName("body") private String body; // Swagger ghi là "body", không phải "message"
    @SerializedName("type") private String type;
    @SerializedName("severity") private String severity; // Dùng để quyết định màu icon (INFO, WARNING...)
    @SerializedName("isRead") private boolean isRead;
    @SerializedName("createdAt") private String createdAt;

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getType() { return type; }
    public String getSeverity() { return severity; }
    public boolean isRead() { return isRead; }
    public String getCreatedAt() { return createdAt; }

    // Setter để update UI cục bộ
    public void setRead(boolean read) { isRead = read; }
}