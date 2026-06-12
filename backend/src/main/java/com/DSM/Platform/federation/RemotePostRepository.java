package com.DSM.Platform.federation;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RemotePostRepository extends JpaRepository<RemotePost, UUID> {

    // join fetch keeps the timeline a single query (server name/baseUrl is
    // needed by the response DTO); explicit countQuery is required with fetch + Pageable.
    @Query(
            value = """
                    select remotePost from RemotePost remotePost
                    join fetch remotePost.server
                    order by remotePost.originalCreatedAt desc, remotePost.syncedAt desc
                    """,
            countQuery = "select count(remotePost) from RemotePost remotePost"
    )
    Page<RemotePost> findTimeline(Pageable pageable);

    @Query("""
            select remotePost.remoteId from RemotePost remotePost
            where remotePost.server.id = :serverId and remotePost.remoteId in :remoteIds
            """)
    Set<UUID> findExistingRemoteIds(@Param("serverId") UUID serverId, @Param("remoteIds") Collection<UUID> remoteIds);

    long countByServerId(UUID serverId);

    void deleteByServerId(UUID serverId);
}
