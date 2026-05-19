package com.attendance.backend.group.dto;

public enum ImportMemberAction {
    CREATED_USER_AND_ADDED,
    EXISTING_USER_ADDED,
    ALREADY_MEMBER_SKIPPED,
    REMOVED_MEMBER_RESTORED,
    VALIDATED_ONLY
}
