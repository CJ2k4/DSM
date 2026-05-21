package com.DSM.Platform.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerLoginRefreshAndMeWork() throws Exception {
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "Charitra",
                                  "email": "charitra@example.com",
                                  "password": "StrongPass123!",
                                  "displayName": "Charitra"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.username").value("charitra"))
                .andExpect(jsonPath("$.user.email").value("charitra@example.com"))
                .andReturn();

        JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String accessToken = registerJson.get("accessToken").asText();
        String refreshToken = registerJson.get("refreshToken").asText();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("charitra"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identifier": "charitra",
                                  "password": "StrongPass123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("charitra@example.com"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString());
    }

    @Test
    void duplicateEmailReturnsConflict() throws Exception {
        String request = """
                {
                  "username": "duplicate_email_user",
                  "email": "duplicate@example.com",
                  "password": "StrongPass123!"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "another_user",
                                  "email": "duplicate@example.com",
                                  "password": "StrongPass123!"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }
}
