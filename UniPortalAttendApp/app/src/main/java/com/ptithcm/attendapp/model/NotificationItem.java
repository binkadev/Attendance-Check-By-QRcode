package com.ptithcm.attendapp.model;
import com.google.gson.annotations.SerializedName;

public class NotificationItem {
    @SerializedName("id") private String id;
    @SerializedName("title") private String title;
    @SerializedName("body") private String body;
    @SerializedName("type") private String type;
    @SerializedName("severity") private String severity;
    @SerializedName("isRead") private boolean isRead;
    @SerializedName("createdAt") private String createdAt;

    // THÊM CÁC TRƯỜNG NÀY ĐỂ XỬ LÝ CLICK
    @SerializedName("sessionId") private String sessionId;
    @SerializedName("sourceType") private String sourceType;
    @SerializedName("sourceRefId") private String sourceRefId;
//    @SerializedName("payload") private String payload;
    @SerializedName("payload") private Object payload;

    // Các Getters cũ...
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getType() { return type; }
    public String getSeverity() { return severity; }
    public boolean isRead() { return isRead; }
    public String getCreatedAt() { return createdAt; }

    // Getters mới
    public String getSessionId() { return sessionId; }
    public String getSourceType() { return sourceType; }
    public String getSourceRefId() { return sourceRefId; }
//    public String getPayload() { return payload; }
    public Object getPayload() { return payload; }

    public void setRead(boolean read) { isRead = read; }
}