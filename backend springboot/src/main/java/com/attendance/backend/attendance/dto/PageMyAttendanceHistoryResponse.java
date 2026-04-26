package com.attendance.backend.attendance.dto;

import java.util.List;

public record PageMyAttendanceHistoryResponse(
        List<MyAttendanceHistoryItemResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}