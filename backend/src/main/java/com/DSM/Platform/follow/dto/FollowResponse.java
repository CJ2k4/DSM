package com.DSM.Platform.follow.dto;

import com.DSM.Platform.user.User;
import java.util.UUID;

public record FollowResponse(
        UUID userId,
        String username,
        boolean following,
        long followerCount,
        long followingCount
) {

    public static FollowResponse from(User target, boolean following, long followerCount, long followingCount) {
        return new FollowResponse(
                target.getId(),
                target.getUsername(),
                following,
                followerCount,
                followingCount
        );
    }
}
