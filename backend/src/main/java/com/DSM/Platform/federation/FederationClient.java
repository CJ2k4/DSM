package com.DSM.Platform.federation;

import com.DSM.Platform.federation.dto.AnnounceRequest;
import com.DSM.Platform.federation.dto.FederationInfoDto;
import com.DSM.Platform.federation.dto.FederationPostPage;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Outbound HTTP calls to peer servers. Built from the autoconfigured
 * RestClient.Builder so customizers (timeouts, test mocks) apply.
 */
@Component
public class FederationClient {

    private final RestClient restClient;

    public FederationClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    public FederationInfoDto fetchInfo(String baseUrl) {
        try {
            return restClient.get()
                    .uri(baseUrl + "/federation/info")
                    .retrieve()
                    .body(FederationInfoDto.class);
        } catch (RestClientException e) {
            throw new FederationClientException("Could not fetch /federation/info from " + baseUrl, e);
        }
    }

    public FederationPostPage fetchPosts(String baseUrl, int page, int size) {
        try {
            return restClient.get()
                    .uri(baseUrl + "/federation/posts?page={page}&size={size}", page, size)
                    .retrieve()
                    .body(FederationPostPage.class);
        } catch (RestClientException e) {
            throw new FederationClientException("Could not fetch /federation/posts from " + baseUrl, e);
        }
    }

    public void announce(String baseUrl, AnnounceRequest self) {
        try {
            restClient.post()
                    .uri(baseUrl + "/federation/announce")
                    .body(self)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new FederationClientException("Could not announce to " + baseUrl, e);
        }
    }
}
