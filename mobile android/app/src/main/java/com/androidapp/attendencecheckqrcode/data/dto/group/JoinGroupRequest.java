package com.androidapp.attendencecheckqrcode.data.dto.group;

import com.google.gson.annotations.SerializedName;

public class JoinGroupRequest {
    @SerializedName("joinCode")
    private String joinCode;

    public JoinGroupRequest(String joinCode) {
        this.joinCode = joinCode;
    }

    // Getter (thêm vào cho chắc chắn)
    public String getJoinCode() {
        return joinCode;
    }
}