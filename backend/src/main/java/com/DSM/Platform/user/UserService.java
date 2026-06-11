package com.DSM.Platform.user;

import com.DSM.Platform.common.exception.ApiException;
import com.DSM.Platform.follow.FollowRepository;
import com.DSM.Platform.security.AuthenticatedUser;
import com.DSM.Platform.user.dto.UpdateProfileRequest;
import com.DSM.Platform.user.dto.UserProfileResponse;
import com.DSM.Platform.user.dto.UserSearchResponse;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;

    public UserService(UserRepository userRepository, FollowRepository followRepository) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(AuthenticatedUser principal) {
        User user = findActiveUserById(principal);
        return toProfileResponse(user, true, false);
    }

    @Transactional
    public UserProfileResponse updateMyProfile(AuthenticatedUser principal, UpdateProfileRequest request) {
        User user = findActiveUserById(principal);

        String displayName = resolveDisplayName(request.displayName(), user.getDisplayName());
        String bio = resolveOptionalText(request.bio(), user.getBio());
        String avatarUrl = resolveOptionalText(request.avatarUrl(), user.getAvatarUrl());
        String bannerUrl = resolveOptionalText(request.bannerUrl(), user.getBannerUrl());

        user.updateProfile(displayName, bio, avatarUrl, bannerUrl);

        return toProfileResponse(user, true, false);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getPublicProfile(String username, UUID viewerId) {
        User user = findActiveUserByUsername(username);
        boolean ownProfile = viewerId != null && viewerId.equals(user.getId());
        boolean following = viewerId != null && !ownProfile
                && followRepository.existsByFollowerIdAndFollowingId(viewerId, user.getId());
        return toProfileResponse(user, ownProfile, following);
    }

    @Transactional(readOnly = true)
    public Page<UserSearchResponse> searchUsers(String query, Pageable pageable) {
        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.length() < 2) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SEARCH_QUERY_TOO_SHORT", "Search query must be at least 2 characters");
        }

        return userRepository.searchActiveUsers(normalizedQuery, pageable)
                .map(UserSearchResponse::from);
    }

    private UserProfileResponse toProfileResponse(User user, boolean ownProfile, boolean following) {
        long followerCount = followRepository.countByFollowingId(user.getId());
        long followingCount = followRepository.countByFollowerId(user.getId());
        return UserProfileResponse.from(user, ownProfile, followerCount, followingCount, following);
    }

    private User findActiveUserById(AuthenticatedUser principal) {
        return userRepository.findById(principal.id())
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
    }

    private User findActiveUserByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(normalizeUsername(username))
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
    }

    private String resolveDisplayName(String requestedDisplayName, String currentDisplayName) {
        if (requestedDisplayName == null) {
            return currentDisplayName;
        }

        String trimmed = requestedDisplayName.trim();
        if (!StringUtils.hasText(trimmed)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DISPLAY_NAME_REQUIRED", "Display name cannot be blank");
        }

        return trimmed;
    }

    private String resolveOptionalText(String requestedValue, String currentValue) {
        if (requestedValue == null) {
            return currentValue;
        }

        String trimmed = requestedValue.trim();
        return StringUtils.hasText(trimmed) ? trimmed : null;
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
