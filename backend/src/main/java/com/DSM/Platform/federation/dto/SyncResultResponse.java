package com.DSM.Platform.federation.dto;

import com.DSM.Platform.federation.FederatedServer;
import com.DSM.Platform.federation.ServerStatus;

/** Per-server outcome of a sync run. */
public record SyncResultResponse(
        String name,
        String baseUrl,
        int fetched,
        ServerStatus status
) {

    public static SyncResultResponse from(FederatedServer server, int fetched) {
        return new SyncResultResponse(server.getName(), server.getBaseUrl(), fetched, server.getStatus());
    }
}
