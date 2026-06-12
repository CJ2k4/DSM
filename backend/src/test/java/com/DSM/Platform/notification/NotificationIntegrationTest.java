package com.DSM.Platform.notification;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "app.federation.auto-sync=false")
@AutoConfigureMockMvc
class NotificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void likeCreatesNotificationForPostOwner() throws Exception {
        String owner = registerAndGetAccessToken("notif_owner_like");
        String liker = registerAndGetAccessToken("notif_liker");
        String postId = createPost(owner, "like me");

        mockMvc.perform(post("/api/v1/posts/" + postId + "/like")
                        .header("Authorization", "Bearer " + liker))
                .andExpect(status().isOk());

        JsonNode notifications = listNotifications(owner);
        JsonNode first = notifications.get("content").get(0);
        Assertions.assertEquals("LIKE", first.get("type").asText());
        Assertions.assertEquals("notif_liker", first.get("actor").get("username").asText());
        Assertions.assertEquals(postId, first.get("postId").asText());
        Assertions.assertFalse(first.get("read").asBoolean());
    }

    @Test
    void duplicateLikeDoesNotDuplicateNotification() throws Exception {
        String owner = registerAndGetAccessToken("notif_owner_dup");
        String liker = registerAndGetAccessToken("notif_liker_dup");
        String postId = createPost(owner, "double tap");

        mockMvc.perform(post("/api/v1/posts/" + postId + "/like")
                        .header("Authorization", "Bearer " + liker))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/posts/" + postId + "/like")
                        .header("Authorization", "Bearer " + liker))
                .andExpect(status().isOk());

        Assertions.assertEquals(1, listNotifications(owner).get("totalElements").asInt());
    }

    @Test
    void commentCreatesNotificationForPostOwner() throws Exception {
        String owner = registerAndGetAccessToken("notif_owner_comment");
        String commenter = registerAndGetAccessToken("notif_commenter");
        String postId = createPost(owner, "discuss");

        mockMvc.perform(post("/api/v1/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + commenter)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "content": "nice post" }
                                """))
                .andExpect(status().isCreated());

        JsonNode first = listNotifications(owner).get("content").get(0);
        Assertions.assertEquals("COMMENT", first.get("type").asText());
        Assertions.assertEquals("notif_commenter", first.get("actor").get("username").asText());
    }

    @Test
    void followCreatesNotificationForTarget() throws Exception {
        String target = registerAndGetAccessToken("notif_target");
        String follower = registerAndGetAccessToken("notif_follower");

        mockMvc.perform(post("/api/v1/users/notif_target/follow")
                        .header("Authorization", "Bearer " + follower))
                .andExpect(status().isOk());

        JsonNode first = listNotifications(target).get("content").get(0);
        Assertions.assertEquals("FOLLOW", first.get("type").asText());
        Assertions.assertEquals("notif_follower", first.get("actor").get("username").asText());
        Assertions.assertTrue(first.get("postId").isNull());
    }

    @Test
    void selfActionsCreateNoNotifications() throws Exception {
        String token = registerAndGetAccessToken("notif_self");
        String postId = createPost(token, "my own post");

        mockMvc.perform(post("/api/v1/posts/" + postId + "/like")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "content": "replying to myself" }
                                """))
                .andExpect(status().isCreated());

        Assertions.assertEquals(0, listNotifications(token).get("totalElements").asInt());
    }

    @Test
    void unreadCountAndMarkAllReadFlow() throws Exception {
        String owner = registerAndGetAccessToken("notif_owner_unread");
        String actor = registerAndGetAccessToken("notif_actor_unread");
        String postId = createPost(owner, "count me");

        mockMvc.perform(post("/api/v1/posts/" + postId + "/like")
                        .header("Authorization", "Bearer " + actor))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/users/notif_owner_unread/follow")
                        .header("Authorization", "Bearer " + actor))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));

        mockMvc.perform(post("/api/v1/notifications/read-all")
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));

        JsonNode first = listNotifications(owner).get("content").get(0);
        Assertions.assertTrue(first.get("read").asBoolean());
    }

    @Test
    void notificationEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/notifications/unread-count"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/notifications/read-all"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/presence"))
                .andExpect(status().isUnauthorized());
    }

    // ---- Helpers ----

    private JsonNode listNotifications(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String createPost(String token, String content) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "content": "%s" }
                                """.formatted(content)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String registerAndGetAccessToken(String username) throws Exception {
        String uniqueEmail = username + "-" + UUID.randomUUID() + "@example.com";

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "email": "%s",
                                  "password": "StrongPass123!",
                                  "displayName": "%s"
                                }
                                """.formatted(username, uniqueEmail, username)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .get("accessToken").asText();
    }
}
