package com.attendance.backend.group.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ImportMembersRequest(
        MemberImportMode importMode,
        MemberImportSyncMode syncMode,
        AccountProvisioningMode accountProvisioningMode,
        Boolean notifyStudents,
        @NotEmpty(message = "members must not be empty")
        @Size(max = 1000, message = "members size must be <= 1000")
        List<@Valid ImportMemberRowRequest> members
) {
    public MemberImportMode effectiveImportMode() {
        return importMode == null ? MemberImportMode.VALIDATE_AND_IMPORT : importMode;
    }

    public MemberImportSyncMode effectiveSyncMode() {
        return syncMode == null ? MemberImportSyncMode.APPEND_ONLY : syncMode;
    }

    public AccountProvisioningMode effectiveAccountProvisioningMode() {
        return accountProvisioningMode == null
                ? AccountProvisioningMode.CREATE_REQUIRE_PASSWORD_CHANGE
                : accountProvisioningMode;
    }

    public boolean effectiveNotifyStudents() {
        return notifyStudents != null && notifyStudents;
    }
}
