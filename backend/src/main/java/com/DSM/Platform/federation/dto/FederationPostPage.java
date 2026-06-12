package com.DSM.Platform.federation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Client-side envelope for a peer's /federation/posts page. Spring's Page
 * type isn't directly deserializable, so this mirrors the fields we use.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FederationPostPage(
        List<FederationPostDto> content,
        int number,
        boolean last,
        long totalElements
) {
}
