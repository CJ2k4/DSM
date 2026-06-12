package com.DSM.Platform.federation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.DSM.Platform.federation.dto.AnnounceRequest;
import com.DSM.Platform.federation.dto.FederationInfoDto;
import com.DSM.Platform.federation.dto.FederationPostPage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

@RestClientTest(FederationClient.class)
class FederationClientTest {

    @Autowired
    private FederationClient federationClient;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void fetchInfoParsesPeerIdentity() {
        server.expect(requestTo("http://peer.example/federation/info"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "name": "Node B",
                          "baseUrl": "http://peer.example",
                          "software": "dsm",
                          "userCount": 3,
                          "postCount": 12,
                          "someFutureField": true
                        }
                        """, MediaType.APPLICATION_JSON));

        FederationInfoDto info = federationClient.fetchInfo("http://peer.example");

        assertEquals("Node B", info.name());
        assertEquals("dsm", info.software());
        assertEquals(12, info.postCount());
    }

    @Test
    void fetchPostsParsesPageEnvelope() {
        server.expect(requestTo("http://peer.example/federation/posts?page=0&size=50"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "content": [
                            {
                              "id": "0b9a4c5e-3f2d-4e8a-9c1b-7d6e5f4a3b2c",
                              "author": {"username": "bob", "displayName": "Bob", "avatarUrl": null},
                              "content": "hello from B",
                              "imageUrl": null,
                              "createdAt": "2026-06-12T05:00:00Z"
                            }
                          ],
                          "number": 0,
                          "last": true,
                          "totalElements": 1
                        }
                        """, MediaType.APPLICATION_JSON));

        FederationPostPage page = federationClient.fetchPosts("http://peer.example", 0, 50);

        assertEquals(1, page.content().size());
        assertTrue(page.last());
        assertEquals("bob", page.content().get(0).author().username());
        assertEquals("hello from B", page.content().get(0).content());
    }

    @Test
    void announceSendsSelfIdentity() {
        server.expect(requestTo("http://peer.example/federation/announce"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.baseUrl").value("http://localhost:8080"))
                .andExpect(jsonPath("$.name").value("Node A"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        federationClient.announce("http://peer.example", new AnnounceRequest("http://localhost:8080", "Node A"));

        server.verify();
    }

    @Test
    void errorsAreWrappedInFederationClientException() {
        server.expect(requestTo("http://peer.example/federation/info"))
                .andRespond(withServerError());

        assertThrows(FederationClientException.class,
                () -> federationClient.fetchInfo("http://peer.example"));
    }
}
