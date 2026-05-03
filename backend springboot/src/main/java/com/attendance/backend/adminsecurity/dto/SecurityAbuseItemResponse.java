package com.attendance.backend.adminsecurity.dto;

public record SecurityAbuseItemResponse(
        String source,
        long totalCount,
        long throttledCount
) {
}