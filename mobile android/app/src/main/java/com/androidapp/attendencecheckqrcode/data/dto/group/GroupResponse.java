package com.androidapp.attendencecheckqrcode.data.dto.group;

import com.google.gson.annotations.SerializedName;

public class GroupResponse {
    @SerializedName("id")
    private String id;
    @SerializedName("name")
    private String name;
    @SerializedName("code")
    private String code;
    @SerializedName("status")
    private String status;

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public String getStatus() { return status; }
}