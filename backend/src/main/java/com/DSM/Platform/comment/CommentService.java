package com.DSM.Platform.comment;

import com.DSM.Platform.comment.dto.CommentResponse;
import com.DSM.Platform.comment.dto.CreateCommentRequest;
import com.DSM.Platform.common.exception.ApiException;
import com.DSM.Platform.notification.NotificationService;
import com.DSM.Platform.notification.NotificationType;
import com.DSM.Platform.post.Post;
import com.DSM.Platform.post.PostRepository;
import com.DSM.Platform.security.AuthenticatedUser;
import com.DSM.Platform.user.User;
import com.DSM.Platform.user.UserRepository;
import com.DSM.Platform.user.UserStatus;
import com.DSM.Platform.websocket.RealtimePublisher;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final RealtimePublisher realtimePublisher;

    public CommentService(
            CommentRepository commentRepository,
            PostRepository postRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            RealtimePublisher realtimePublisher
    ) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.realtimePublisher = realtimePublisher;
    }

    @Transactional
    public CommentResponse addComment(AuthenticatedUser principal, UUID postId, CreateCommentRequest request) {
        Post post = findPost(postId);
        User author = findActiveUserById(principal.id());

        Comment comment = commentRepository.save(new Comment(post, author, request.content().trim()));
        CommentResponse response = CommentResponse.from(comment);

        notificationService.notify(post.getAuthor(), author, NotificationType.COMMENT, postId);
        realtimePublisher.broadcastComment(postId, response);

        return response;
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getComments(UUID postId, Pageable pageable) {
        findPost(postId);
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId, pageable)
                .map(CommentResponse::from);
    }

    @Transactional
    public void deleteComment(AuthenticatedUser principal, UUID commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", "Comment not found"));

        boolean isCommentAuthor = comment.getAuthor().getId().equals(principal.id());
        boolean isPostOwner = comment.getPost().getAuthor().getId().equals(principal.id());
        if (!isCommentAuthor && !isPostOwner) {
            throw new ApiException(HttpStatus.FORBIDDEN, "COMMENT_FORBIDDEN",
                    "You can only delete your own comments or comments on your posts");
        }

        commentRepository.delete(comment);
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
