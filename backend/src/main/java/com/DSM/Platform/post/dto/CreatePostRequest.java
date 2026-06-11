package com.DSM.Platform.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
        @NotBlank(message = "Content is required")
        @Size(max = 1000, message = "Content must be 1000 characters or fewer")
        String content,

        @Size(max = 2048, message = "Image URL must be 2048 characters or fewer")
        String imageUrl
) {
}
