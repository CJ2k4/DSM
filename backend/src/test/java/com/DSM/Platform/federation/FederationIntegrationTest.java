package com.DSM.Platform.federation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.DSM.Platform.federation.dto.FederationAuthorDto;
import com.DSM.Platform.federation.dto.FederationInfoDto;
import com.DSM.Platform.federation.dto.FederationPostDto;
import com.DSM.Platform.federation.dto.FederationPostPage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "app.federation.auto-sync=false")
@AutoConfigureMockMvc
class FederationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FederatedServerRepository serverRepository;

    @Autowired
    private RemotePostRepository remotePostRepository;

    // Outbound HTTP is mocked: tests drive peers via stubbed responses.
    @MockBean
    private FederationClient federationClient;

    // ---- Public protocol endpoints ----

    @Test
    void infoIsPublicAndDescribesThisNode() throws Exception {
        mockMvc.perform(get("/federation/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.software").value("dsm"))
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.baseUrl").exists());
    }

    @Test
    void postsAreExportedPubliclyWithAuthor() throws Exception {
        String token = registerAndGetAccessToken("fed_export_user");
        mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "content": "exported across the network" }
                                """))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/federation/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertTrue(
                body.contains("exported across the network"), "export should contain the new post");
        org.junit.jupiter.api.Assertions.assertTrue(
                body.contains("fed_export_user"), "export should embed author info");
    }

    @Test
    void usersAreExportedPublicly() throws Exception {
        registerAndGetAccessToken("fed_user_export");

        MvcResult result = mockMvc.perform(get("/federation/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andReturn();

        org.junit.jupiter.api.Assertions.assertTrue(
                result.getResponse().getContentAsString().contains("fed_user_export"));
    }

    @Test
    void announceRegistersPeerIdempotently() throws Exception {
        String baseUrl = "http://announce-peer.example";

        mockMvc.perform(post("/federation/announce")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "baseUrl": "%s/", "name": "Announcer" }
                                """.formatted(baseUrl)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.software").value("dsm"));

        // Repeat with a new name: still one row, name updated.
        mockMvc.perform(post("/federation/announce")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "baseUrl": "%s", "name": "Announcer v2" }
                                """.formatted(baseUrl)))
                .andExpect(status().isOk());

        FederatedServer saved = serverRepository.findByBaseUrl(baseUrl).orElseThrow();
        assertEquals("Announcer v2", saved.getName());
    }

    @Test
    void announceRejectsSelf() throws Exception {
        mockMvc.perform(post("/federation/announce")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "baseUrl": "http://localhost:8080", "name": "Me" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FEDERATION_CANNOT_ADD_SELF"));
    }

    // ---- Authenticated peer management ----

    @Test
    void adminEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/federation/servers"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/federation/sync"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/federation/timeline"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addServerVerifiesPeerPullsPostsAndListsIt() throws Exception {
        String token = registerAndGetAccessToken("fed_admin");
        String peerUrl = "http://node-b.example";
        stubHealthyPeer(peerUrl, "Node B", 2);

        mockMvc.perform(post("/api/v1/federation/servers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "baseUrl": "%s/" }
                                """.formatted(peerUrl)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.baseUrl").value(peerUrl))
                .andExpect(jsonPath("$.name").value("Node B"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.remotePostCount").value(2));

        mockMvc.perform(get("/api/v1/federation/servers")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.baseUrl == '%s')].name".formatted(peerUrl)).value("Node B"));
    }

    @Test
    void addServerFailsWhenPeerUnreachable() throws Exception {
        String token = registerAndGetAccessToken("fed_admin_unreachable");
        when(federationClient.fetchInfo(anyString()))
                .thenThrow(new FederationClientException("connection refused"));

        mockMvc.perform(post("/api/v1/federation/servers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "baseUrl": "http://down.example" }
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("FEDERATION_SERVER_UNREACHABLE"));
    }

    @Test
    void addServerRejectsDuplicateAndSelf() throws Exception {
        String token = registerAndGetAccessToken("fed_admin_dup");
        String peerUrl = "http://dup-peer.example";
        stubHealthyPeer(peerUrl, "Dup Peer", 0);

        mockMvc.perform(post("/api/v1/federation/servers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "baseUrl": "%s" }
                                """.formatted(peerUrl)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/federation/servers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "baseUrl": "%s/" }
                                """.formatted(peerUrl)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FEDERATION_SERVER_EXISTS"));

        mockMvc.perform(post("/api/v1/federation/servers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "baseUrl": "http://localhost:8080/" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FEDERATION_CANNOT_ADD_SELF"));
    }

    @Test
    void addServerRejectsInvalidUrl() throws Exception {
        String token = registerAndGetAccessToken("fed_admin_badurl");

        mockMvc.perform(post("/api/v1/federation/servers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "baseUrl": "not a url" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FEDERATION_INVALID_URL"));
    }

    // ---- Sync & timeline ----

    @Test
    void syncIsIdempotentAndMarksUnreachablePeers() throws Exception {
        String token = registerAndGetAccessToken("fed_admin_sync");
        String peerUrl = "http://sync-peer.example";
        stubHealthyPeer(peerUrl, "Sync Peer", 3);

        mockMvc.perform(post("/api/v1/federation/servers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "baseUrl": "%s" }
                                """.formatted(peerUrl)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.remotePostCount").value(3));

        // Same posts again: nothing new is stored.
        MvcResult syncResult = mockMvc.perform(post("/api/v1/federation/sync")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String body = syncResult.getResponse().getContentAsString();
        var results = objectMapper.readTree(body);
        var thisPeer = findByBaseUrl(results, peerUrl);
        assertEquals(0, thisPeer.get("fetched").asInt(), "re-sync should fetch nothing new");
        assertEquals("ACTIVE", thisPeer.get("status").asText());

        // Peer goes down: status flips to UNREACHABLE, sync still returns 200.
        when(federationClient.fetchPosts(eq(peerUrl), anyInt(), anyInt()))
                .thenThrow(new FederationClientException("connection refused"));

        MvcResult downResult = mockMvc.perform(post("/api/v1/federation/sync")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        var downPeer = findByBaseUrl(objectMapper.readTree(downResult.getResponse().getContentAsString()), peerUrl);
        assertEquals("UNREACHABLE", downPeer.get("status").asText());
    }

    @Test
    void timelineReturnsRemotePostsNewestFirstWithServerInfo() throws Exception {
        String token = registerAndGetAccessToken("fed_admin_timeline");
        String peerUrl = "http://timeline-peer.example";

        Instant older = Instant.parse("2026-06-10T10:00:00Z");
        Instant newer = Instant.parse("2026-06-11T10:00:00Z");
        when(federationClient.fetchInfo(eq(peerUrl)))
                .thenReturn(new FederationInfoDto("Timeline Peer", peerUrl, "dsm", 1, 2));
        when(federationClient.fetchPosts(eq(peerUrl), anyInt(), anyInt()))
                .thenReturn(new FederationPostPage(List.of(
                        remotePost("older post from peer", older),
                        remotePost("newer post from peer", newer)
                ), 0, true, 2));

        mockMvc.perform(post("/api/v1/federation/servers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "baseUrl": "%s" }
                                """.formatted(peerUrl)))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/api/v1/federation/timeline")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        var timeline = objectMapper.readTree(result.getResponse().getContentAsString()).get("content");
        // Find this peer's posts in order (other tests' posts may be interleaved).
        String newerSeenAt = null;
        String olderSeenAt = null;
        for (int i = 0; i < timeline.size(); i++) {
            var node = timeline.get(i);
            if (node.get("content").asText().equals("newer post from peer")) {
                newerSeenAt = String.valueOf(i);
                assertEquals("Timeline Peer", node.get("server").get("name").asText());
                assertEquals("peer_author", node.get("author").get("username").asText());
            }
            if (node.get("content").asText().equals("older post from peer")) {
                olderSeenAt = String.valueOf(i);
            }
        }
        org.junit.jupiter.api.Assertions.assertNotNull(newerSeenAt, "newer post should be in the timeline");
        org.junit.jupiter.api.Assertions.assertNotNull(olderSeenAt, "older post should be in the timeline");
        org.junit.jupiter.api.Assertions.assertTrue(
                Integer.parseInt(newerSeenAt) < Integer.parseInt(olderSeenAt),
                "newer remote post should rank before older");
    }

    @Test
    void removingServerDeletesItsRemotePosts() throws Exception {
        String token = registerAndGetAccessToken("fed_admin_remove");
        String peerUrl = "http://remove-peer.example";
        stubHealthyPeer(peerUrl, "Remove Peer", 2);

        MvcResult added = mockMvc.perform(post("/api/v1/federation/servers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "baseUrl": "%s" }
                                """.formatted(peerUrl)))
                .andExpect(status().isCreated())
                .andReturn();
        String serverId = objectMapper.readTree(added.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(delete("/api/v1/federation/servers/" + serverId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertEquals(0, remotePostRepository.countByServerId(UUID.fromString(serverId)));
        mockMvc.perform(delete("/api/v1/federation/servers/" + serverId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FEDERATION_SERVER_NOT_FOUND"));
    }

    // ---- Helpers ----

    private void stubHealthyPeer(String baseUrl, String name, int postCount) {
        List<FederationPostDto> posts = new java.util.ArrayList<>();
        for (int i = 0; i < postCount; i++) {
            posts.add(remotePost("peer post " + UUID.randomUUID(), Instant.now().minusSeconds(i)));
        }
        when(federationClient.fetchInfo(eq(baseUrl)))
                .thenReturn(new FederationInfoDto(name, baseUrl, "dsm", 1, postCount));
        when(federationClient.fetchPosts(eq(baseUrl), anyInt(), anyInt()))
                .thenReturn(new FederationPostPage(posts, 0, true, postCount));
        // announce(...) is a void stub: Mockito no-ops it by default.
    }

    private FederationPostDto remotePost(String content, Instant createdAt) {
        return new FederationPostDto(
                UUID.randomUUID(),
                new FederationAuthorDto("peer_author", "Peer Author", null),
                content,
                null,
                createdAt
        );
    }

    private com.fasterxml.jackson.databind.JsonNode findByBaseUrl(
            com.fasterxml.jackson.databind.JsonNode results, String baseUrl) {
        for (var node : results) {
            if (node.get("baseUrl").asText().equals(baseUrl)) {
                return node;
            }
        }
        throw new AssertionError("No sync result for " + baseUrl + " in " + results);
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
