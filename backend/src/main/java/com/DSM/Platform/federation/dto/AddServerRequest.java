package com.DSM.Platform.federation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddServerRequest(
        @NotBlank(message = "Base URL is required")
        @Size(max = 512, message = "Base URL must be 512 characters or fewer")
        String baseUrl
) {
}
