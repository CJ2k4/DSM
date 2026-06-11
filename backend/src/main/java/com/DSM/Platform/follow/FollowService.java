package com.DSM.Platform.follow;

import com.DSM.Platform.common.exception.ApiException;
import com.DSM.Platform.follow.dto.FollowResponse;
import com.DSM.Platform.security.AuthenticatedUser;
import com.DSM.Platform.user.User;
import com.DSM.Platform.user.UserRepository;
import com.DSM.Platform.user.UserStatus;
import com.DSM.Platform.user.dto.UserSearchResponse;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    public FollowService(FollowRepository followRepository, UserRepository userRepository) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public FollowResponse follow(AuthenticatedUser principal, String username) {
        User target = findActiveUserByUsername(username);

        if (target.getId().equals(principal.id())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CANNOT_FOLLOW_SELF", "You cannot follow yourself");
        }

        if (!followRepository.existsByFollowerIdAndFollowingId(principal.id(), target.getId())) {
            User follower = findActiveUserById(principal.id());
            followRepository.save(new Follow(follower, target));
        }

        return buildResponse(target, true);
    }

    @Transactional
    public FollowResponse unfollow(AuthenticatedUser principal, String username) {
        User target = findActiveUserByUsername(username);

        followRepository.findByFollowerIdAndFollowingId(principal.id(), target.getId())
                .ifPresent(followRepository::delete);

        return buildResponse(target, false);
    }

    @Transactional(readOnly = true)
    public Page<UserSearchResponse> getFollowers(String username, Pageable pageable) {
        User target = findActiveUserByUsername(username);
        return followRepository.findFollowersOf(target.getId(), pageable)
                .map(UserSearchResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<UserSearchResponse> getFollowing(String username, Pageable pageable) {
        User target = findActiveUserByUsername(username);
        return followRepository.findFollowingOf(target.getId(), pageable)
                .map(UserSearchResponse::from);
    }

    private FollowResponse buildResponse(User target, boolean following) {
        long followerCount = followRepository.countByFollowingId(target.getId());
        long followingCount = followRepository.countByFollowerId(target.getId());
        return FollowResponse.from(target, following, followerCount, followingCount);
    }

    private User findActiveUserById(UUID id) {
        return userRepository.findById(id)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
    }

    private User findActiveUserByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(normalizeUsername(username))
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
