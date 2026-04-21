package com.attendance.backend.group.dto;

import com.attendance.backend.domain.enums.GroupStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateGroupStatusRequest {

    @NotNull(message = "status is required")
    private GroupStatus status;

    public UpdateGroupStatusRequest() {
    }

    public GroupStatus getStatus() {
        return status;
    }

    public void setStatus(GroupStatus status) {
        this.status = status;
    }
}