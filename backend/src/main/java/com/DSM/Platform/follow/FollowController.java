package com.DSM.Platform.follow;

import com.DSM.Platform.follow.dto.FollowResponse;
import com.DSM.Platform.security.AuthenticatedUser;
import com.DSM.Platform.user.dto.UserSearchResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @PostMapping("/{username}/follow")
    public FollowResponse follow(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String username
    ) {
        return followService.follow(principal, username);
    }

    @DeleteMapping("/{username}/follow")
    public FollowResponse unfollow(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String username
    ) {
        return followService.unfollow(principal, username);
    }

    @GetMapping("/{username}/followers")
    public Page<UserSearchResponse> followers(
            @PathVariable String username,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return followService.getFollowers(username, pageable);
    }

    @GetMapping("/{username}/following")
    public Page<UserSearchResponse> following(
            @PathVariable String username,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return followService.getFollowing(username, pageable);
    }
}
