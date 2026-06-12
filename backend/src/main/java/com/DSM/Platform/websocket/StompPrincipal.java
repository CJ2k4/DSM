package com.DSM.Platform.websocket;

import java.security.Principal;
import java.util.UUID;

/**
 * Identity of a STOMP session. {@code getName()} is the user's UUID string —
 * it's what convertAndSendToUser() routes /user/queue/* messages by.
 */
public record StompPrincipal(UUID userId, String username) implements Principal {

    @Override
    public String getName() {
        return userId.toString();
    }
}
