package com.DSM.Platform.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
        @Pattern(regexp = "^[A-Za-z0-9_.]+$", message = "Username can contain letters, numbers, underscores, and dots")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 320, message = "Email is too long")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        String password,

        @Size(max = 80, message = "Display name must be 80 characters or fewer")
        String displayName
) {
}
