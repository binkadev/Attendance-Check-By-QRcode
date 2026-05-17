package com.ptithcm.attendapp.model;
import com.google.gson.annotations.SerializedName;

public class AttendanceSummaryResponse {
    @SerializedName("overallRate")
    private double overallRate; // Ví dụ: 91.3

    @SerializedName("presentCount")
    private int presentCount;   // Ví dụ: 42

    @SerializedName("lateCount")
    private int lateCount;      // Ví dụ: 3

    @SerializedName("absentCount")
    private int absentCount;    // Ví dụ: 1

    public double getOverallRate() { return overallRate; }
    public int getPresentCount() { return presentCount; }
    public int getLateCount() { return lateCount; }
    public int getAbsentCount() { return absentCount; }
}