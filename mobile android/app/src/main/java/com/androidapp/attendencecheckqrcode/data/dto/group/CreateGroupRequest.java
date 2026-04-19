package com.androidapp.attendencecheckqrcode.data.dto.group;

import com.google.gson.annotations.SerializedName;

public class CreateGroupRequest {
    @SerializedName("name")
    private String name;
    @SerializedName("code")
    private String code;
    @SerializedName("joinCode")
    private String joinCode;
    @SerializedName("description")
    private String description;
    @SerializedName("semester")
    private String semester;
    @SerializedName("room")
    private String room;
    @SerializedName("approvalMode")
    private String approvalMode;
    @SerializedName("allowAutoJoinOnCheckin")
    private boolean allowAutoJoinOnCheckin;

    public CreateGroupRequest(String name, String code, String joinCode, String description,
                              String semester, String room, String approvalMode, boolean allowAutoJoinOnCheckin) {
        this.name = name;
        this.code = code;
        this.joinCode = joinCode;
        this.description = description;
        this.semester = semester;
        this.room = room;
        this.approvalMode = approvalMode;
        this.allowAutoJoinOnCheckin = allowAutoJoinOnCheckin;
    }
}