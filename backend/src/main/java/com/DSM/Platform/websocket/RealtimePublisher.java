package com.DSM.Platform.websocket;

import com.DSM.Platform.comment.dto.CommentResponse;
import com.DSM.Platform.notification.dto.NotificationResponse;
import java.util.UUID;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/** Single place that knows the STOMP destinations the frontend subscribes to. */
@Component
public class RealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public RealtimePublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /** Per-user notification queue: /user/queue/notifications. */
    public void sendNotification(UUID recipientId, NotificationResponse notification) {
        messagingTemplate.convertAndSendToUser(recipientId.toString(), "/queue/notifications", notification);
    }

    /** Per-post live comment feed: /topic/posts/{postId}/comments. */
    public void broadcastComment(UUID postId, CommentResponse comment) {
        messagingTemplate.convertAndSend("/topic/posts/" + postId + "/comments", comment);
    }

    /** Presence snapshot: /topic/presence. */
    public void broadcastPresence(Object snapshot) {
        messagingTemplate.convertAndSend("/topic/presence", snapshot);
    }
}
