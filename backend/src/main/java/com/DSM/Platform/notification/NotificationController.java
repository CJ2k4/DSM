package com.DSM.Platform.notification;

import com.DSM.Platform.notification.dto.NotificationResponse;
import com.DSM.Platform.security.AuthenticatedUser;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public Page<NotificationResponse> list(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return notificationService.getNotifications(principal, pageable);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal AuthenticatedUser principal) {
        return Map.of("count", notificationService.unreadCount(principal));
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(@AuthenticationPrincipal AuthenticatedUser principal) {
        notificationService.markAllRead(principal);
    }
}
