package com.DSM.Platform.comment.dto;

import com.DSM.Platform.comment.Comment;
import com.DSM.Platform.user.dto.UserSearchResponse;
import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        String content,
        UserSearchResponse author,
        Instant createdAt
) {

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                UserSearchResponse.from(comment.getAuthor()),
                comment.getCreatedAt()
        );
    }
}
