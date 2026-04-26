package com.attendance.backend.session.dto;

import java.util.List;

public record PageSessionResponse(
        List<SessionResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}