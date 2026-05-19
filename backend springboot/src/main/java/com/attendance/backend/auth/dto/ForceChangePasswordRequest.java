package com.attendance.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForceChangePasswordRequest(
        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        @Size(max = 190, message = "email must be <= 190 characters")
        String email,

        @NotBlank(message = "currentPassword is required")
        @Size(max = 200, message = "currentPassword must be <= 200 characters")
        String currentPassword,

        @NotBlank(message = "newPassword is required")
        @Size(min = 8, max = 200, message = "newPassword must be between 8 and 200 characters")
        String newPassword,

        @Size(max = 120, message = "deviceId must be <= 120 characters")
        String deviceId
) {
}
