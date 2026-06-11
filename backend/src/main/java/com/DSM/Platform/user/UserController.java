package com.DSM.Platform.user;

import com.DSM.Platform.security.AuthenticatedUser;
import com.DSM.Platform.user.dto.UpdateProfileRequest;
import com.DSM.Platform.user.dto.UserProfileResponse;
import com.DSM.Platform.user.dto.UserSearchResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserProfileResponse getMyProfile(@AuthenticationPrincipal AuthenticatedUser principal) {
        return userService.getMyProfile(principal);
    }

    @PatchMapping("/me")
    public UserProfileResponse updateMyProfile(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return userService.updateMyProfile(principal, request);
    }

    @GetMapping("/search")
    public Page<UserSearchResponse> searchUsers(
            @RequestParam("q")
            @NotBlank(message = "Search query is required")
            @Size(max = 80, message = "Search query must be 80 characters or fewer")
            String query,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return userService.searchUsers(query, pageable);
    }

    @GetMapping("/{username}")
    public UserProfileResponse getPublicProfile(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String username
    ) {
        return userService.getPublicProfile(username, principal != null ? principal.id() : null);
    }
}
