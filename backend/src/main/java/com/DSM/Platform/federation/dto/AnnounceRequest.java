package com.DSM.Platform.federation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A peer announcing itself so federation can become mutual. */
public record AnnounceRequest(
        @NotBlank(message = "Base URL is required")
        @Size(max = 512, message = "Base URL must be 512 characters or fewer")
        String baseUrl,

        @Size(max = 80, message = "Name must be 80 characters or fewer")
        String name
) {
}
