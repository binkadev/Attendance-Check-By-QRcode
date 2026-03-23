package com.attendance.backend.adminsecurity.api.dto;

public record SecurityAbuseItemResponse(
        String source,
        long totalCount,
        long throttledCount
) {
}