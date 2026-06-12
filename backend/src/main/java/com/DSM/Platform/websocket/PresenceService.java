package com.DSM.Platform.websocket;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Tracks who has an open WebSocket session and broadcasts the online list
 * whenever it changes. A user with multiple tabs counts once.
 */
@Service
public class PresenceService {

    public record PresenceSnapshot(int count, List<String> usernames) {
    }

    private final Map<String, StompPrincipal> sessions = new ConcurrentHashMap<>();
    private final RealtimePublisher realtimePublisher;

    public PresenceService(RealtimePublisher realtimePublisher) {
        this.realtimePublisher = realtimePublisher;
    }

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        if (accessor.getSessionId() != null && event.getUser() instanceof StompPrincipal principal) {
            sessions.put(accessor.getSessionId(), principal);
            realtimePublisher.broadcastPresence(snapshot());
        }
    }

    @EventListener
    public void onDisconnected(SessionDisconnectEvent event) {
        if (sessions.remove(event.getSessionId()) != null) {
            realtimePublisher.broadcastPresence(snapshot());
        }
    }

    public PresenceSnapshot snapshot() {
        List<String> usernames = sessions.values().stream()
                .map(StompPrincipal::username)
                .distinct()
                .sorted()
                .toList();
        return new PresenceSnapshot(usernames.size(), usernames);
    }
}
