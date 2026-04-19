package com.androidapp.attendencecheckqrcode.data.dto.teaching;

import com.google.gson.annotations.SerializedName;

public class AttendancePolicyRequest {
    @SerializedName("lateWeight")
    private int lateWeight = 1;

    @SerializedName("warningBelowRate")
    private int warningBelowRate = 0;

    @SerializedName("criticalBelowRate")
    private int criticalBelowRate = 0;

    @SerializedName("warningAbsentCount")
    private int warningAbsentCount;

    @SerializedName("criticalAbsentCount")
    private int criticalAbsentCount;

    public AttendancePolicyRequest(int maxAbsence) {
        // Cảnh báo ở buổi vắng kề cuối, cấm thi ở buổi vắng tối đa
        this.warningAbsentCount = Math.max(0, maxAbsence - 1);
        this.criticalAbsentCount = maxAbsence;
    }
}