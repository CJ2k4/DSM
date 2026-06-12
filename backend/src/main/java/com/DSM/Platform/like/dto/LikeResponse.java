package com.DSM.Platform.like.dto;

import java.util.UUID;

public record LikeResponse(
        UUID postId,
        boolean liked,
        long likeCount
) {
}
