package com.ptithcm.attendapp.model;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class UpcomingSection {
    @SerializedName("key") private String key;
    @SerializedName("title") private String title;
    @SerializedName("date") private String date;
    @SerializedName("items") private List<UpcomingSessionItem> items;

    // 👇 THÊM HÀM NÀY
    public String getKey() { return key; }

    public String getDate() { return date; }
    public List<UpcomingSessionItem> getItems() { return items; }
}