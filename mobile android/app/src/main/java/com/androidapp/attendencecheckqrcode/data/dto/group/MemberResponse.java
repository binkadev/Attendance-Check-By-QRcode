package com.androidapp.attendencecheckqrcode.data.dto.group;

import com.google.gson.annotations.SerializedName;

public class MemberResponse {
    @SerializedName("groupId")
    private String groupId;

    @SerializedName("userId")
    private String userId;

    @SerializedName("role")
    private String role;

    @SerializedName("memberStatus")
    private String memberStatus;

    @SerializedName("fullName")
    private String fullName;

    public String getGroupId() { return groupId; }
    public String getUserId() { return userId; }
    public String getRole() { return role; }
    public String getMemberStatus() { return memberStatus; }
    public String getFullName() { return fullName; }
}