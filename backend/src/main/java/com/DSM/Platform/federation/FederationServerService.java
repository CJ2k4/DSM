package com.DSM.Platform.federation;

import com.DSM.Platform.common.exception.ApiException;
import com.DSM.Platform.federation.dto.AddServerRequest;
import com.DSM.Platform.federation.dto.AnnounceRequest;
import com.DSM.Platform.federation.dto.FederationInfoDto;
import com.DSM.Platform.federation.dto.RemotePostResponse;
import com.DSM.Platform.federation.dto.ServerResponse;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FederationServerService {

    private static final Logger log = LoggerFactory.getLogger(FederationServerService.class);

    private final FederatedServerRepository serverRepository;
    private final RemotePostRepository remotePostRepository;
    private final FederationClient federationClient;
    private final FederationSyncService syncService;
    private final FederationProperties properties;

    public FederationServerService(
            FederatedServerRepository serverRepository,
            RemotePostRepository remotePostRepository,
            FederationClient federationClient,
            FederationSyncService syncService,
            FederationProperties properties
    ) {
        this.serverRepository = serverRepository;
        this.remotePostRepository = remotePostRepository;
        this.federationClient = federationClient;
        this.syncService = syncService;
        this.properties = properties;
    }

    /**
     * Registers a peer: verify it speaks the federation protocol, save it,
     * then best-effort announce ourselves and pull its posts. Announce/pull
     * failures never roll back the registration.
     */
    public ServerResponse addServer(AddServerRequest request) {
        String baseUrl = normalizeBaseUrl(request.baseUrl());
        rejectSelf(baseUrl);
        if (serverRepository.existsByBaseUrl(baseUrl)) {
            throw new ApiException(HttpStatus.CONFLICT, "FEDERATION_SERVER_EXISTS",
                    "Server is already registered: " + baseUrl);
        }

        FederationInfoDto info;
        try {
            info = federationClient.fetchInfo(baseUrl);
        } catch (FederationClientException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "FEDERATION_SERVER_UNREACHABLE",
                    "Could not reach a DSM federation endpoint at " + baseUrl);
        }
        if (info == null || info.name() == null) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "FEDERATION_SERVER_UNREACHABLE",
                    "Server at " + baseUrl + " did not return valid federation info");
        }

        FederatedServer server = serverRepository.save(new FederatedServer(baseUrl, info.name()));

        try {
            federationClient.announce(baseUrl, selfAnnouncement());
        } catch (FederationClientException e) {
            log.warn("Announce to {} failed (registration kept): {}", baseUrl, e.getMessage());
        }
        try {
            syncService.syncServer(server);
            server = serverRepository.findById(server.getId()).orElse(server);
        } catch (Exception e) {
            log.warn("Initial sync from {} failed (registration kept): {}", baseUrl, e.getMessage());
        }

        return ServerResponse.from(server, remotePostRepository.countByServerId(server.getId()));
    }

    /** Handles an inbound peer announcement: upsert only, never announce back. */
    @Transactional
    public void registerAnnouncedPeer(AnnounceRequest request) {
        String baseUrl = normalizeBaseUrl(request.baseUrl());
        rejectSelf(baseUrl);
        String name = request.name() != null && !request.name().isBlank()
                ? request.name()
                : baseUrl;
        serverRepository.findByBaseUrl(baseUrl).ifPresentOrElse(
                existing -> existing.rename(name),
                () -> serverRepository.save(new FederatedServer(baseUrl, name))
        );
    }

    public List<ServerResponse> listServers() {
        return serverRepository.findAll().stream()
                .map(server -> ServerResponse.from(server, remotePostRepository.countByServerId(server.getId())))
                .toList();
    }

    @Transactional
    public void removeServer(UUID id) {
        FederatedServer server = serverRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "FEDERATION_SERVER_NOT_FOUND",
                        "Federated server not found"));
        remotePostRepository.deleteByServerId(server.getId());
        serverRepository.delete(server);
    }

    @Transactional(readOnly = true)
    public Page<RemotePostResponse> getTimeline(Pageable pageable) {
        return remotePostRepository.findTimeline(pageable).map(RemotePostResponse::from);
    }

    public AnnounceRequest selfAnnouncement() {
        return new AnnounceRequest(normalizeBaseUrl(properties.getSelfBaseUrl()), properties.getServerName());
    }

    private void rejectSelf(String baseUrl) {
        if (baseUrl.equalsIgnoreCase(normalizeBaseUrl(properties.getSelfBaseUrl()))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "FEDERATION_CANNOT_ADD_SELF",
                    "A server cannot federate with itself");
        }
    }

    /** Lowercases scheme/host and strips trailing slashes; rejects non-http(s) URLs. */
    static String normalizeBaseUrl(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        URI uri;
        try {
            uri = URI.create(trimmed);
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "FEDERATION_INVALID_URL",
                    "Base URL is not a valid URL");
        }
        String scheme = uri.getScheme();
        if (scheme == null || uri.getHost() == null
                || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "FEDERATION_INVALID_URL",
                    "Base URL must be an absolute http(s) URL");
        }
        String port = uri.getPort() == -1 ? "" : ":" + uri.getPort();
        String path = uri.getRawPath() == null ? "" : uri.getRawPath();
        return scheme.toLowerCase() + "://" + uri.getHost().toLowerCase() + port + path;
    }
}
