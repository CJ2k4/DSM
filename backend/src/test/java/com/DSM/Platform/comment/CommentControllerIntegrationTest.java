package com.DSM.Platform.comment;

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
class CommentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void addCommentListsItAndUpdatesCount() throws Exception {
        String authorToken = registerAndGetAccessToken("comment_author");
        String commenterToken = registerAndGetAccessToken("comment_commenter");
        String postId = createPost(authorToken, "comment on me");

        mockMvc.perform(post("/api/v1/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + commenterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "content": "nice post!" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("nice post!"))
                .andExpect(jsonPath("$.author.username").value("comment_commenter"));

        // Public listing, oldest-first.
        mockMvc.perform(get("/api/v1/posts/" + postId + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].content").value("nice post!"));

        // commentCount reflected on the post.
        mockMvc.perform(get("/api/v1/posts/user/comment_author"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].commentCount").value(1));
    }

    @Test
    void commentAuthorCanDeleteOwnComment() throws Exception {
        String authorToken = registerAndGetAccessToken("cmt_owner_author");
        String commenterToken = registerAndGetAccessToken("cmt_owner_commenter");
        String postId = createPost(authorToken, "post");
        String commentId = addComment(commenterToken, postId, "my comment");

        mockMvc.perform(delete("/api/v1/comments/" + commentId)
                        .header("Authorization", "Bearer " + commenterToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void postOwnerCanDeleteSomeoneElsesComment() throws Exception {
        String authorToken = registerAndGetAccessToken("mod_post_author");
        String commenterToken = registerAndGetAccessToken("mod_commenter");
        String postId = createPost(authorToken, "moderated post");
        String commentId = addComment(commenterToken, postId, "to be moderated");

        // The post author (not the comment author) removes the comment.
        mockMvc.perform(delete("/api/v1/comments/" + commentId)
                        .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void thirdPartyCannotDeleteComment() throws Exception {
        String authorToken = registerAndGetAccessToken("tp_post_author");
        String commenterToken = registerAndGetAccessToken("tp_commenter");
        String strangerToken = registerAndGetAccessToken("tp_stranger");
        String postId = createPost(authorToken, "post");
        String commentId = addComment(commenterToken, postId, "not yours");

        mockMvc.perform(delete("/api/v1/comments/" + commentId)
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMMENT_FORBIDDEN"));
    }

    @Test
    void deleteUnknownCommentReturnsNotFound() throws Exception {
        String token = registerAndGetAccessToken("cmt_missing_user");

        mockMvc.perform(delete("/api/v1/comments/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));
    }

    @Test
    void commentOnUnknownPostReturnsNotFound() throws Exception {
        String token = registerAndGetAccessToken("cmt_no_post_user");

        mockMvc.perform(post("/api/v1/posts/" + UUID.randomUUID() + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "content": "hi" }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    @Test
    void addCommentRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/posts/" + UUID.randomUUID() + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "content": "hi" }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private String addComment(String token, String postId, String content) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "content": "%s" }
                                """.formatted(content)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
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
