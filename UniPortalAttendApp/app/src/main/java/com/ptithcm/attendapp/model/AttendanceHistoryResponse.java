package com.ptithcm.attendapp.model;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AttendanceHistoryResponse {
    @SerializedName("items")
    private List<AttendanceHistoryItem> items;
    @SerializedName("totalElements")
    private int totalElements;

    public List<AttendanceHistoryItem> getItems() { return items; }
    public int getTotalElements() { return totalElements; }
}