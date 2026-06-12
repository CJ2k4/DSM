package com.DSM.Platform.federation.dto;

import com.DSM.Platform.user.User;
import java.time.Instant;

/** Public wire format for a user profile shared with peer servers. */
public record FederationUserDto(
        String username,
        String displayName,
        String bio,
        String avatarUrl,
        Instant createdAt
) {

    public static FederationUserDto from(User user) {
        return new FederationUserDto(
                user.getUsername(),
                user.getDisplayName(),
                user.getBio(),
                user.getAvatarUrl(),
                user.getCreatedAt()
        );
    }
}
