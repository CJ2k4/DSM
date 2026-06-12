package com.DSM.Platform.federation.dto;

import com.DSM.Platform.federation.FederatedServer;
import com.DSM.Platform.federation.ServerStatus;
import java.time.Instant;
import java.util.UUID;

public record ServerResponse(
        UUID id,
        String baseUrl,
        String name,
        ServerStatus status,
        Instant lastSyncAt,
        long remotePostCount,
        Instant createdAt
) {

    public static ServerResponse from(FederatedServer server, long remotePostCount) {
        return new ServerResponse(
                server.getId(),
                server.getBaseUrl(),
                server.getName(),
                server.getStatus(),
                server.getLastSyncAt(),
                remotePostCount,
                server.getCreatedAt()
        );
    }
}
