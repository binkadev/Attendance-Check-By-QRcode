package com.attendance.backend.group.dto;

public record MemberImportValidationError(
        int rowIndex,
        String field,
        String code,
        String message
) {
}
