package com.attendance.backend.group.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ImportMemberRowRequest(
        Integer rowIndex,

        @NotBlank(message = "studentCode is required")
        @Size(max = 40, message = "studentCode must be <= 40 characters")
        String studentCode,

        @NotBlank(message = "fullName is required")
        @Size(max = 120, message = "fullName must be <= 120 characters")
        String fullName,

        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        @Size(max = 190, message = "email must be <= 190 characters")
        String email
) {
}
