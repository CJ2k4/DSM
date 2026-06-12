package com.DSM.Platform.like;

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
class LikeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void likeIncrementsCountAndIsIdempotentThenUnlikeResets() throws Exception {
        String authorToken = registerAndGetAccessToken("like_author");
        String likerToken = registerAndGetAccessToken("like_liker");
        String postId = createPost(authorToken, "like me");

        mockMvc.perform(post("/api/v1/posts/" + postId + "/like")
                        .header("Authorization", "Bearer " + likerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(true))
                .andExpect(jsonPath("$.likeCount").value(1));

        // Idempotent: liking again keeps the count at 1.
        mockMvc.perform(post("/api/v1/posts/" + postId + "/like")
                        .header("Authorization", "Bearer " + likerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(1));

        mockMvc.perform(delete("/api/v1/posts/" + postId + "/like")
                        .header("Authorization", "Bearer " + likerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(false))
                .andExpect(jsonPath("$.likeCount").value(0));
    }

    @Test
    void likeIsReflectedInPostResponse() throws Exception {
        String authorToken = registerAndGetAccessToken("reflect_author");
        String likerToken = registerAndGetAccessToken("reflect_liker");
        String postId = createPost(authorToken, "reflected post");

        mockMvc.perform(post("/api/v1/posts/" + postId + "/like")
                        .header("Authorization", "Bearer " + likerToken))
                .andExpect(status().isOk());

        // As the liker, the post's likedByMe should be true and likeCount 1.
        mockMvc.perform(get("/api/v1/posts/user/reflect_author")
                        .header("Authorization", "Bearer " + likerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].likeCount").value(1))
                .andExpect(jsonPath("$.content[0].likedByMe").value(true));

        // Unauthenticated viewer: likedByMe false, count still 1.
        mockMvc.perform(get("/api/v1/posts/user/reflect_author"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].likeCount").value(1))
                .andExpect(jsonPath("$.content[0].likedByMe").value(false));
    }

    @Test
    void likeUnknownPostReturnsNotFound() throws Exception {
        String token = registerAndGetAccessToken("like_missing_user");

        mockMvc.perform(post("/api/v1/posts/" + UUID.randomUUID() + "/like")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    @Test
    void likeRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/posts/" + UUID.randomUUID() + "/like"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
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
