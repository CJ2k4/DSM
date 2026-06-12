package com.DSM.Platform.post;

import com.DSM.Platform.post.dto.CreatePostRequest;
import com.DSM.Platform.post.dto.PostResponse;
import com.DSM.Platform.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse create(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody CreatePostRequest request
    ) {
        return postService.createPost(principal, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id
    ) {
        postService.deletePost(principal, id);
    }

    @GetMapping("/feed")
    public Page<PostResponse> feed(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return postService.getFeed(principal, pageable);
    }

    @GetMapping("/user/{username}")
    public Page<PostResponse> userPosts(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String username,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return postService.getUserPosts(username, principal != null ? principal.id() : null, pageable);
    }
}
