package com.ptithcm.attendapp.model;

public class JoinGroupRequest {
    private String joinCode;

    public JoinGroupRequest(String joinCode) {
        this.joinCode = joinCode;
    }

    public String getJoinCode() {
        return joinCode;
    }

    public void setJoinCode(String joinCode) {
        this.joinCode = joinCode;
    }
}