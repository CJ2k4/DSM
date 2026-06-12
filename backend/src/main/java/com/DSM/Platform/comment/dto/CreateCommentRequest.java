package com.DSM.Platform.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank(message = "Content is required")
        @Size(max = 500, message = "Content must be 500 characters or fewer")
        String content
) {
}
