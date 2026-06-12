package com.DSM.Platform.notification;

import com.DSM.Platform.notification.dto.NotificationResponse;
import com.DSM.Platform.security.AuthenticatedUser;
import com.DSM.Platform.user.User;
import com.DSM.Platform.websocket.RealtimePublisher;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final RealtimePublisher realtimePublisher;

    public NotificationService(
            NotificationRepository notificationRepository,
            RealtimePublisher realtimePublisher
    ) {
        this.notificationRepository = notificationRepository;
        this.realtimePublisher = realtimePublisher;
    }

    /**
     * Records a notification and pushes it to the recipient's live queue.
     * Self-actions (liking your own post, etc.) are silently skipped.
     */
    @Transactional
    public void notify(User recipient, User actor, NotificationType type, UUID postId) {
        if (recipient.getId().equals(actor.getId())) {
            return;
        }
        Notification saved = notificationRepository.save(new Notification(recipient, actor, type, postId));
        realtimePublisher.sendNotification(recipient.getId(), NotificationResponse.from(saved));
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(AuthenticatedUser principal, Pageable pageable) {
        return notificationRepository.findForRecipient(principal.id(), pageable)
                .map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public long unreadCount(AuthenticatedUser principal) {
        return notificationRepository.countByRecipientIdAndReadFalse(principal.id());
    }

    @Transactional
    public void markAllRead(AuthenticatedUser principal) {
        notificationRepository.markAllRead(principal.id());
    }
}
