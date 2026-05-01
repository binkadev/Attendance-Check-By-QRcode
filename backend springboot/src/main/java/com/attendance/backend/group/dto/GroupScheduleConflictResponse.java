package com.attendance.backend.group.dto;

import java.util.List;

public record GroupScheduleConflictResponse(
        boolean valid,
        List<GroupScheduleConflictItemResponse> conflicts
) {
}
