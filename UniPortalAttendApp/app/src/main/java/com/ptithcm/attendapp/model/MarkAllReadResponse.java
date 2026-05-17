package com.ptithcm.attendapp.model;
import com.google.gson.annotations.SerializedName;

public class MarkAllReadResponse {
    @SerializedName("updatedCount") private int updatedCount;
    public int getUpdatedCount() { return updatedCount; }
}