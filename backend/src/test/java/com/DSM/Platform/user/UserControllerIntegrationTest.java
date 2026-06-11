package com.DSM.Platform.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAndUpdateMyProfileWork() throws Exception {
        String accessToken = registerAndGetAccessToken("profile_user");

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("profile_user"))
                .andExpect(jsonPath("$.ownProfile").value(true));

        mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Profile User",
                                  "bio": "Building DSM one feature at a time.",
                                  "avatarUrl": "https://cdn.example.com/avatar.png",
                                  "bannerUrl": "https://cdn.example.com/banner.png"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Profile User"))
                .andExpect(jsonPath("$.bio").value("Building DSM one feature at a time."))
                .andExpect(jsonPath("$.avatarUrl").value("https://cdn.example.com/avatar.png"))
                .andExpect(jsonPath("$.bannerUrl").value("https://cdn.example.com/banner.png"))
                .andExpect(jsonPath("$.ownProfile").value(true));
    }

    @Test
    void myProfileRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void publicProfileCanBeViewedByUsername() throws Exception {
        registerAndGetAccessToken("public_profile_user");

        mockMvc.perform(get("/api/v1/users/public_profile_user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("public_profile_user"))
                .andExpect(jsonPath("$.ownProfile").value(false));
    }

    @Test
    void unknownPublicProfileReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/users/missing_user"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void userSearchReturnsMatchingProfiles() throws Exception {
        registerAndGetAccessToken("searchable_user");

        mockMvc.perform(get("/api/v1/users/search")
                        .param("q", "searchable")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("searchable_user"));
    }

    @Test
    void profileUpdateRejectsBlankDisplayName() throws Exception {
        String accessToken = registerAndGetAccessToken("blank_display_user");

        mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DISPLAY_NAME_REQUIRED"));
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
