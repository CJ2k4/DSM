package com.DSM.Platform.federation.dto;

import com.DSM.Platform.post.Post;
import java.time.Instant;
import java.util.UUID;

/** Public wire format for a post shared with peer servers. */
public record FederationPostDto(
        UUID id,
        FederationAuthorDto author,
        String content,
        String imageUrl,
        Instant createdAt
) {

    public static FederationPostDto from(Post post) {
        return new FederationPostDto(
                post.getId(),
                FederationAuthorDto.from(post.getAuthor()),
                post.getContent(),
                post.getImageUrl(),
                post.getCreatedAt()
        );
    }
}
