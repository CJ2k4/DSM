package com.DSM.Platform.follow;

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
class FollowControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void followAndUnfollowUpdateStateAndCounts() throws Exception {
        String aliceToken = registerAndGetAccessToken("follow_alice");
        registerAndGetAccessToken("follow_bob");

        mockMvc.perform(post("/api/v1/users/follow_bob/follow")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("follow_bob"))
                .andExpect(jsonPath("$.following").value(true))
                .andExpect(jsonPath("$.followerCount").value(1));

        mockMvc.perform(get("/api/v1/users/follow_bob")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.following").value(true))
                .andExpect(jsonPath("$.followerCount").value(1));

        mockMvc.perform(get("/api/v1/users/follow_bob/followers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("follow_alice"));

        mockMvc.perform(get("/api/v1/users/follow_alice/following"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("follow_bob"));

        mockMvc.perform(delete("/api/v1/users/follow_bob/follow")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.following").value(false))
                .andExpect(jsonPath("$.followerCount").value(0));
    }

    @Test
    void followingIsIdempotent() throws Exception {
        String aliceToken = registerAndGetAccessToken("idem_alice");
        registerAndGetAccessToken("idem_bob");

        mockMvc.perform(post("/api/v1/users/idem_bob/follow")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/users/idem_bob/follow")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.following").value(true))
                .andExpect(jsonPath("$.followerCount").value(1));
    }

    @Test
    void cannotFollowSelf() throws Exception {
        String aliceToken = registerAndGetAccessToken("self_alice");

        mockMvc.perform(post("/api/v1/users/self_alice/follow")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_FOLLOW_SELF"));
    }

    @Test
    void publicProfileFollowingFlagIsFalseWhenUnauthenticated() throws Exception {
        String aliceToken = registerAndGetAccessToken("anon_alice");
        registerAndGetAccessToken("anon_bob");

        mockMvc.perform(post("/api/v1/users/anon_bob/follow")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/anon_bob"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.following").value(false))
                .andExpect(jsonPath("$.followerCount").value(1));
    }

    @Test
    void followRequiresAuthentication() throws Exception {
        registerAndGetAccessToken("unauth_target");

        mockMvc.perform(post("/api/v1/users/unauth_target/follow"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void followingUnknownUserReturnsNotFound() throws Exception {
        String aliceToken = registerAndGetAccessToken("missing_target_alice");

        mockMvc.perform(post("/api/v1/users/no_such_user/follow")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
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
