package com.DSM.Platform.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 80, message = "Display name must be 80 characters or fewer")
        String displayName,

        @Size(max = 500, message = "Bio must be 500 characters or fewer")
        String bio,

        @Size(max = 2048, message = "Avatar URL must be 2048 characters or fewer")
        String avatarUrl,

        @Size(max = 2048, message = "Banner URL must be 2048 characters or fewer")
        String bannerUrl
) {
}
