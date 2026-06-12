package com.DSM.Platform.federation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

/**
 * A local copy of a post pulled from a federated peer. Uniqueness on
 * (server, remoteId) makes repeated syncs idempotent.
 */
@Entity
@Table(
        name = "remote_posts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_remote_posts_server_remote",
                columnNames = {"server_id", "remote_id"}
        ),
        indexes = @Index(name = "idx_remote_posts_original_created_at", columnList = "original_created_at")
)
public class RemotePost {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "server_id", nullable = false, updatable = false)
    private FederatedServer server;

    /** The post's id on its origin server. */
    @Column(name = "remote_id", nullable = false, updatable = false)
    private UUID remoteId;

    @Column(name = "author_username", nullable = false, length = 80)
    private String authorUsername;

    @Column(name = "author_display_name", nullable = false, length = 80)
    private String authorDisplayName;

    @Column(name = "author_avatar_url", length = 2048)
    private String authorAvatarUrl;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(length = 2048)
    private String imageUrl;

    @Column(name = "original_created_at", nullable = false)
    private Instant originalCreatedAt;

    @CreationTimestamp
    @Column(name = "synced_at", nullable = false, updatable = false)
    private Instant syncedAt;

    protected RemotePost() {
    }

    public RemotePost(
            FederatedServer server,
            UUID remoteId,
            String authorUsername,
            String authorDisplayName,
            String authorAvatarUrl,
            String content,
            String imageUrl,
            Instant originalCreatedAt
    ) {
        this.server = server;
        this.remoteId = remoteId;
        this.authorUsername = authorUsername;
        this.authorDisplayName = authorDisplayName;
        this.authorAvatarUrl = authorAvatarUrl;
        this.content = content;
        this.imageUrl = imageUrl;
        this.originalCreatedAt = originalCreatedAt;
    }

    public UUID getId() {
        return id;
    }

    public FederatedServer getServer() {
        return server;
    }

    public UUID getRemoteId() {
        return remoteId;
    }

    public String getAuthorUsername() {
        return authorUsername;
    }

    public String getAuthorDisplayName() {
        return authorDisplayName;
    }

    public String getAuthorAvatarUrl() {
        return authorAvatarUrl;
    }

    public String getContent() {
        return content;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Instant getOriginalCreatedAt() {
        return originalCreatedAt;
    }

    public Instant getSyncedAt() {
        return syncedAt;
    }
}
