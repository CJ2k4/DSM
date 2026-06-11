package com.DSM.Platform.like;

import com.DSM.Platform.common.exception.ApiException;
import com.DSM.Platform.like.dto.LikeResponse;
import com.DSM.Platform.post.Post;
import com.DSM.Platform.post.PostRepository;
import com.DSM.Platform.security.AuthenticatedUser;
import com.DSM.Platform.user.User;
import com.DSM.Platform.user.UserRepository;
import com.DSM.Platform.user.UserStatus;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LikeService {

    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public LikeService(LikeRepository likeRepository, PostRepository postRepository, UserRepository userRepository) {
        this.likeRepository = likeRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public LikeResponse like(AuthenticatedUser principal, UUID postId) {
        Post post = findPost(postId);

        if (!likeRepository.existsByPostIdAndUserId(postId, principal.id())) {
            User user = findActiveUserById(principal.id());
            likeRepository.save(new Like(post, user));
        }

        return buildResponse(postId, true);
    }

    @Transactional
    public LikeResponse unlike(AuthenticatedUser principal, UUID postId) {
        findPost(postId);

        likeRepository.findByPostIdAndUserId(postId, principal.id())
                .ifPresent(likeRepository::delete);

        return buildResponse(postId, false);
    }

    private LikeResponse buildResponse(UUID postId, boolean liked) {
        return new LikeResponse(postId, liked, likeRepository.countByPostId(postId));
    }

    private Post findPost(UUID postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "Post not found"));
    }

    private User findActiveUserById(UUID id) {
        return userRepository.findById(id)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
    }
}
