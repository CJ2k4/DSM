package com.DSM.Platform.post;

import com.DSM.Platform.comment.CommentRepository;
import com.DSM.Platform.common.exception.ApiException;
import com.DSM.Platform.like.LikeRepository;
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
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;

    public PostService(
            PostRepository postRepository,
            UserRepository userRepository,
            LikeRepository likeRepository,
            CommentRepository commentRepository
    ) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
    }

    @Transactional
    public PostResponse createPost(AuthenticatedUser principal, CreatePostRequest request) {
        User author = findActiveUserById(principal.id());

        String imageUrl = StringUtils.hasText(request.imageUrl()) ? request.imageUrl().trim() : null;
        Post post = postRepository.save(new Post(author, request.content().trim(), imageUrl));

        return toResponse(post, principal.id());
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
                .map(post -> toResponse(post, principal.id()));
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getUserPosts(String username, UUID viewerId, Pageable pageable) {
        User author = findActiveUserByUsername(username);
        return postRepository.findByAuthorIdOrderByCreatedAtDesc(author.getId(), pageable)
                .map(post -> toResponse(post, viewerId));
    }

    private PostResponse toResponse(Post post, UUID viewerId) {
        long likeCount = likeRepository.countByPostId(post.getId());
        long commentCount = commentRepository.countByPostId(post.getId());
        boolean likedByMe = viewerId != null && likeRepository.existsByPostIdAndUserId(post.getId(), viewerId);
        return PostResponse.from(post, likeCount, commentCount, likedByMe);
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
