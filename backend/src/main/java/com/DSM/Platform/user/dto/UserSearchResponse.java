package com.DSM.Platform.user.dto;

import com.DSM.Platform.user.User;
import java.util.UUID;

public record UserSearchResponse(
        UUID id,
        String username,
        String displayName,
        String bio,
        String avatarUrl
) {

    public static UserSearchResponse from(User user) {
        return new UserSearchResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getBio(),
                user.getAvatarUrl()
        );
    }
}
