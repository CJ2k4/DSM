package com.DSM.Platform.websocket;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.federation.auto-sync=false"
)
class WebSocketRealtimeIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private ObjectMapper objectMapper;

    private StompSession session;

    @AfterEach
    void disconnect() {
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }

    @Test
    void connectsWithValidJwtAndReceivesLiveNotification() throws Exception {
        String ownerToken = register("ws_owner");
        String likerToken = register("ws_liker");
        String postId = createPost(ownerToken, "realtime test post");

        session = connect(ownerToken);
        assertTrue(session.isConnected(), "owner should connect over STOMP");

        CompletableFuture<String> received = new CompletableFuture<>();
        session.subscribe("/user/queue/notifications", new StompFrameHandler() {
            // Raw bytes: the server sends application/json, which the string
            // converter would silently reject.
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.complete(new String((byte[]) payload, java.nio.charset.StandardCharsets.UTF_8));
            }
        });
        // Give the broker a beat to register the subscription.
        Thread.sleep(300);

        HttpHeaders headers = authJson(likerToken);
        rest.postForEntity("/api/v1/posts/" + postId + "/like", new HttpEntity<>(null, headers), String.class);

        String payload = received.get(10, TimeUnit.SECONDS);
        JsonNode notification = objectMapper.readTree(payload);
        org.junit.jupiter.api.Assertions.assertEquals("LIKE", notification.get("type").asText());
        org.junit.jupiter.api.Assertions.assertEquals("ws_liker", notification.get("actor").get("username").asText());
    }

    @Test
    void rejectsConnectWithoutToken() {
        assertThrows(ExecutionException.class, () -> connectRaw(null));
    }

    @Test
    void rejectsConnectWithGarbageToken() {
        assertThrows(ExecutionException.class, () -> connectRaw("not-a-jwt"));
    }

    // ---- Helpers ----

    private StompSession connect(String token) throws Exception {
        return connectRaw(token);
    }

    private StompSession connectRaw(String token) throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        StompHeaders connectHeaders = new StompHeaders();
        if (token != null) {
            connectHeaders.add("Authorization", "Bearer " + token);
        }
        return client.connectAsync(
                "ws://localhost:" + port + "/ws",
                new WebSocketHttpHeaders(),
                connectHeaders,
                new StompSessionHandlerAdapter() {
                }
        ).get(10, TimeUnit.SECONDS);
    }

    private String register(String username) throws Exception {
        Map<String, String> body = Map.of(
                "username", username,
                "email", username + "-" + java.util.UUID.randomUUID() + "@example.com",
                "password", "StrongPass123!",
                "displayName", username
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String response = rest.postForEntity("/api/v1/auth/register", new HttpEntity<>(body, headers), String.class)
                .getBody();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private String createPost(String token, String content) throws Exception {
        HttpHeaders headers = authJson(token);
        String response = rest.postForEntity("/api/v1/posts",
                        new HttpEntity<>(Map.of("content", content), headers), String.class)
                .getBody();
        return objectMapper.readTree(response).get("id").asText();
    }

    private HttpHeaders authJson(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }
}
