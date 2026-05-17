package com.ptithcm.attendapp.model;

public class JoinGroupResponse {
    private String groupId;
    private String role;
    private String memberStatus; // Trạng thái: APPROVED hoặc PENDING
    private String fullName;

    public String getGroupId() { return groupId; }
    public String getRole() { return role; }
    public String getMemberStatus() { return memberStatus; }
    public String getFullName() { return fullName; }
}