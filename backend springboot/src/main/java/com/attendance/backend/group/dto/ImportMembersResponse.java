package com.attendance.backend.group.dto;

import java.util.List;
import java.util.UUID;

public record ImportMembersResponse(
        UUID groupId,
        int totalRows,
        int createdUsers,
        int linkedExistingUsers,
        int addedMembers,
        int skippedExistingMembers,
        int restoredMembers,
        int invitationEmailsQueued,
        List<ImportMemberItemResponse> items
) {
}
