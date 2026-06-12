package com.DSM.Platform.websocket;

import com.DSM.Platform.security.JwtService;
import com.DSM.Platform.user.User;
import com.DSM.Platform.user.UserRepository;
import com.DSM.Platform.user.UserStatus;
import java.util.UUID;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * Authenticates STOMP CONNECT frames with the same JWT used for REST calls
 * (sent as an "Authorization: Bearer ..." STOMP header). The HTTP handshake
 * itself is open; a session without a valid token is rejected here.
 */
@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtChannelInterceptor(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            accessor.setUser(authenticate(accessor.getFirstNativeHeader("Authorization")));
        }
        return message;
    }

    private StompPrincipal authenticate(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing Authorization header on STOMP CONNECT");
        }
        String token = authorizationHeader.substring(7);
        UUID userId;
        try {
            userId = jwtService.extractUserId(token);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid token on STOMP CONNECT");
        }
        User user = userRepository.findById(userId)
                .filter(found -> found.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Unknown user on STOMP CONNECT"));
        return new StompPrincipal(user.getId(), user.getUsername());
    }
}
