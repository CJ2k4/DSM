package com.DSM.Platform.federation.dto;

import com.DSM.Platform.user.User;

public record FederationAuthorDto(
        String username,
        String displayName,
        String avatarUrl
) {

    public static FederationAuthorDto from(User user) {
        return new FederationAuthorDto(user.getUsername(), user.getDisplayName(), user.getAvatarUrl());
    }
}
