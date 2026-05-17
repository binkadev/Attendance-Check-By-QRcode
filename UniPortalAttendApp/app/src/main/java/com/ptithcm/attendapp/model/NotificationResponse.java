package com.ptithcm.attendapp.model;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class NotificationResponse {
    @SerializedName("items") private List<NotificationItem> items;
    @SerializedName("totalElements") private int totalElements;

    public List<NotificationItem> getItems() { return items; }
    public int getTotalElements() { return totalElements; }
}