package com.DSM.Platform.user.dto;

import com.DSM.Platform.user.User;
import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String username,
        String displayName,
        String bio,
        String avatarUrl,
        String bannerUrl,
        Instant createdAt,
        boolean ownProfile,
        long followerCount,
        long followingCount,
        boolean following
) {

    public static UserProfileResponse from(
            User user,
            boolean ownProfile,
            long followerCount,
            long followingCount,
            boolean following
    ) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getBio(),
                user.getAvatarUrl(),
                user.getBannerUrl(),
                user.getCreatedAt(),
                ownProfile,
                followerCount,
                followingCount,
                following
        );
    }
}
