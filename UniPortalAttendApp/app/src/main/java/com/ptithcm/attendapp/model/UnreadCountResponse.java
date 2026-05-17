package com.ptithcm.attendapp.model;
import com.google.gson.annotations.SerializedName;

public class UnreadCountResponse {
    @SerializedName("unreadCount") private int unreadCount;
    public int getUnreadCount() { return unreadCount; }
}