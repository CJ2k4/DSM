package com.DSM.Platform.post;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class PostControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createPostReturnsCreatedWithBody() throws Exception {
        String token = registerAndGetAccessToken("post_author");

        mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "Hello DSM!",
                                  "imageUrl": "https://cdn.example.com/p.png"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Hello DSM!"))
                .andExpect(jsonPath("$.imageUrl").value("https://cdn.example.com/p.png"))
                .andExpect(jsonPath("$.author.username").value("post_author"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void createPostRejectsBlankContent() throws Exception {
        String token = registerAndGetAccessToken("blank_post_author");

        mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "content": "   " }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPostRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "content": "no token" }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void feedContainsFollowedAndOwnPostsButNotStrangers() throws Exception {
        String aliceToken = registerAndGetAccessToken("feed_alice");
        String bobToken = registerAndGetAccessToken("feed_bob");
        String carolToken = registerAndGetAccessToken("feed_carol");

        // Alice follows Bob (but not Carol).
        mockMvc.perform(post("/api/v1/users/feed_bob/follow")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk());

        createPost(bobToken, "post from bob");
        createPost(carolToken, "post from carol");
        createPost(aliceToken, "post from alice");

        MvcResult feedResult = mockMvc.perform(get("/api/v1/posts/feed")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode feed = objectMapper.readTree(feedResult.getResponse().getContentAsString());
        String contents = feed.get("content").toString();
        org.junit.jupiter.api.Assertions.assertTrue(contents.contains("post from bob"), "feed should contain followed user's post");
        org.junit.jupiter.api.Assertions.assertTrue(contents.contains("post from alice"), "feed should contain own post");
        org.junit.jupiter.api.Assertions.assertFalse(contents.contains("post from carol"), "feed should not contain non-followed user's post");
    }

    @Test
    void deleteOwnPostRemovesIt() throws Exception {
        String token = registerAndGetAccessToken("delete_owner");
        String postId = createPost(token, "to be deleted");

        mockMvc.perform(delete("/api/v1/posts/" + postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Second delete should now be a 404 (already gone).
        mockMvc.perform(delete("/api/v1/posts/" + postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    @Test
    void cannotDeleteAnotherUsersPost() throws Exception {
        String ownerToken = registerAndGetAccessToken("real_owner");
        String otherToken = registerAndGetAccessToken("not_owner");
        String postId = createPost(ownerToken, "owner's post");

        mockMvc.perform(delete("/api/v1/posts/" + postId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("POST_FORBIDDEN"));
    }

    @Test
    void deleteUnknownPostReturnsNotFound() throws Exception {
        String token = registerAndGetAccessToken("missing_post_user");

        mockMvc.perform(delete("/api/v1/posts/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    @Test
    void userPostsAreListedPublicly() throws Exception {
        String token = registerAndGetAccessToken("listed_user");
        createPost(token, "listed user post");

        mockMvc.perform(get("/api/v1/posts/user/listed_user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].content").value("listed user post"))
                .andExpect(jsonPath("$.content[0].author.username").value("listed_user"));
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

        JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        return registerJson.get("accessToken").asText();
    }
}
