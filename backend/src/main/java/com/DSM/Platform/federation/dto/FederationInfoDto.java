package com.DSM.Platform.federation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Identity card a node serves at /federation/info. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FederationInfoDto(
        String name,
        String baseUrl,
        String software,
        long userCount,
        long postCount
) {
}
