package com.DSM.Platform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Email or username is required")
        String identifier,

        @NotBlank(message = "Password is required")
        @Size(max = 128, message = "Password is too long")
        String password
) {
}
