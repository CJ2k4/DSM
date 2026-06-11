package com.DSM.Platform.post;

import com.DSM.Platform.common.exception.ApiException;
import com.DSM.Platform.post.dto.CreatePostRequest;
import com.DSM.Platform.post.dto.PostResponse;
import com.DSM.Platform.security.AuthenticatedUser;
import com.DSM.Platform.user.User;
import com.DSM.Platform.user.UserRepository;
import com.DSM.Platform.user.UserStatus;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public PostService(PostRepository postRepository, UserRepository userRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public PostResponse createPost(AuthenticatedUser principal, CreatePostRequest request) {
        User author = findActiveUserById(principal.id());

        String imageUrl = StringUtils.hasText(request.imageUrl()) ? request.imageUrl().trim() : null;
        Post post = postRepository.save(new Post(author, request.content().trim(), imageUrl));

        return PostResponse.from(post);
    }

    @Transactional
    public void deletePost(AuthenticatedUser principal, UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "Post not found"));

        if (!post.getAuthor().getId().equals(principal.id())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "POST_FORBIDDEN", "You can only delete your own posts");
        }

        postRepository.delete(post);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getFeed(AuthenticatedUser principal, Pageable pageable) {
        return postRepository.findFeed(principal.id(), pageable)
                .map(PostResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getUserPosts(String username, Pageable pageable) {
        User author = findActiveUserByUsername(username);
        return postRepository.findByAuthorIdOrderByCreatedAtDesc(author.getId(), pageable)
                .map(PostResponse::from);
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
