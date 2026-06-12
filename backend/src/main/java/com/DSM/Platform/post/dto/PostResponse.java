package com.DSM.Platform.post.dto;

import com.DSM.Platform.post.Post;
import com.DSM.Platform.user.dto.UserSearchResponse;
import java.time.Instant;
import java.util.UUID;

public record PostResponse(
        UUID id,
        String content,
        String imageUrl,
        UserSearchResponse author,
        Instant createdAt,
        long likeCount,
        long commentCount,
        boolean likedByMe
) {

    public static PostResponse from(Post post, long likeCount, long commentCount, boolean likedByMe) {
        return new PostResponse(
                post.getId(),
                post.getContent(),
                post.getImageUrl(),
                UserSearchResponse.from(post.getAuthor()),
                post.getCreatedAt(),
                likeCount,
                commentCount,
                likedByMe
        );
    }
}
