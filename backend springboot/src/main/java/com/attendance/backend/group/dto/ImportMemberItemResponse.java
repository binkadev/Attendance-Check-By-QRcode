package com.attendance.backend.group.dto;

import java.util.UUID;

public record ImportMemberItemResponse(
        int rowIndex,
        String studentCode,
        String fullName,
        String email,
        UUID userId,
        String memberStatus,
        String accountStatus,
        ImportMemberAction action,
        String loginIdentifier,
        String defaultPasswordRule
) {
}
