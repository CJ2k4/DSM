package com.DSM.Platform.notification.dto;

import com.DSM.Platform.notification.Notification;
import com.DSM.Platform.notification.NotificationType;
import com.DSM.Platform.user.dto.UserSearchResponse;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        UserSearchResponse actor,
        UUID postId,
        boolean read,
        Instant createdAt
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                UserSearchResponse.from(notification.getActor()),
                notification.getPostId(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
