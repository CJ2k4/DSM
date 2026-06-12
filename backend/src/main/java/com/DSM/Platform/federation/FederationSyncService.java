package com.DSM.Platform.federation;

import com.DSM.Platform.federation.dto.FederationPostDto;
import com.DSM.Platform.federation.dto.FederationPostPage;
import com.DSM.Platform.federation.dto.SyncResultResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Pulls posts from registered peers and stores them as RemotePost rows.
 * Repository calls run in their own transactions, so one failing peer (or a
 * race between scheduled and manual sync) never affects the others.
 */
@Service
public class FederationSyncService {

    private static final Logger log = LoggerFactory.getLogger(FederationSyncService.class);
    private static final int PAGE_SIZE = 50;

    private final FederatedServerRepository serverRepository;
    private final RemotePostRepository remotePostRepository;
    private final FederationClient federationClient;
    private final FederationProperties properties;

    public FederationSyncService(
            FederatedServerRepository serverRepository,
            RemotePostRepository remotePostRepository,
            FederationClient federationClient,
            FederationProperties properties
    ) {
        this.serverRepository = serverRepository;
        this.remotePostRepository = remotePostRepository;
        this.federationClient = federationClient;
        this.properties = properties;
    }

    public List<SyncResultResponse> syncAll() {
        List<SyncResultResponse> results = new ArrayList<>();
        for (FederatedServer server : serverRepository.findAll()) {
            int fetched = 0;
            try {
                fetched = syncServer(server);
            } catch (Exception e) {
                // e.g. a unique-constraint race with a concurrent sync — skip,
                // the next run self-heals.
                log.warn("Sync from {} failed: {}", server.getBaseUrl(), e.getMessage());
            }
            FederatedServer refreshed = serverRepository.findById(server.getId()).orElse(server);
            results.add(SyncResultResponse.from(refreshed, fetched));
        }
        return results;
    }

    /**
     * Pulls pages from one peer until its last page or the per-sync cap.
     * Already-synced posts (by remoteId) are skipped, making this idempotent.
     */
    public int syncServer(FederatedServer server) {
        int fetched = 0;
        int page = 0;
        try {
            while (fetched < properties.getMaxPostsPerSync()) {
                FederationPostPage postPage = federationClient.fetchPosts(server.getBaseUrl(), page, PAGE_SIZE);
                if (postPage == null || postPage.content() == null || postPage.content().isEmpty()) {
                    break;
                }
                fetched += storeNewPosts(server, postPage.content());
                if (postPage.last()) {
                    break;
                }
                page += 1;
            }
            server.markSynced();
        } catch (FederationClientException e) {
            log.warn("Peer {} unreachable during sync: {}", server.getBaseUrl(), e.getMessage());
            server.markUnreachable();
        } finally {
            serverRepository.save(server);
        }
        return fetched;
    }

    private int storeNewPosts(FederatedServer server, List<FederationPostDto> posts) {
        List<UUID> remoteIds = posts.stream().map(FederationPostDto::id).toList();
        Set<UUID> existing = remotePostRepository.findExistingRemoteIds(server.getId(), remoteIds);

        List<RemotePost> fresh = posts.stream()
                .filter(post -> post.id() != null && !existing.contains(post.id()))
                .filter(post -> post.content() != null && post.author() != null)
                .map(post -> new RemotePost(
                        server,
                        post.id(),
                        post.author().username(),
                        post.author().displayName() != null ? post.author().displayName() : post.author().username(),
                        post.author().avatarUrl(),
                        post.content(),
                        post.imageUrl(),
                        post.createdAt()
                ))
                .toList();

        if (!fresh.isEmpty()) {
            remotePostRepository.saveAll(fresh);
        }
        return fresh.size();
    }
}
