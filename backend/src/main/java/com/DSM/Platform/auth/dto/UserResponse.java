package com.DSM.Platform.auth.dto;

import com.DSM.Platform.user.User;
import com.DSM.Platform.user.UserRole;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        String displayName,
        String bio,
        String avatarUrl,
        String bannerUrl,
        UserRole role,
        boolean emailVerified,
        Instant createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getBio(),
                user.getAvatarUrl(),
                user.getBannerUrl(),
                user.getRole(),
                user.isEmailVerified(),
                user.getCreatedAt()
        );
    }
}
