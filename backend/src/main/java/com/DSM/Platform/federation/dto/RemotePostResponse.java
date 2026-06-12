package com.DSM.Platform.federation.dto;

import com.DSM.Platform.federation.RemotePost;
import java.time.Instant;
import java.util.UUID;

/** Federated timeline entry served to this node's own frontend. */
public record RemotePostResponse(
        UUID id,
        String content,
        String imageUrl,
        RemoteAuthor author,
        RemoteServer server,
        Instant createdAt
) {

    public record RemoteAuthor(String username, String displayName, String avatarUrl) {
    }

    public record RemoteServer(String name, String baseUrl) {
    }

    public static RemotePostResponse from(RemotePost remotePost) {
        return new RemotePostResponse(
                remotePost.getId(),
                remotePost.getContent(),
                remotePost.getImageUrl(),
                new RemoteAuthor(
                        remotePost.getAuthorUsername(),
                        remotePost.getAuthorDisplayName(),
                        remotePost.getAuthorAvatarUrl()
                ),
                new RemoteServer(
                        remotePost.getServer().getName(),
                        remotePost.getServer().getBaseUrl()
                ),
                remotePost.getOriginalCreatedAt()
        );
    }
}
