package com.DSM.Platform.federation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
        name = "federated_servers",
        uniqueConstraints = @UniqueConstraint(name = "uk_federated_servers_base_url", columnNames = "base_url")
)
public class FederatedServer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    /** Normalized peer base URL (no trailing slash), e.g. http://localhost:8081. */
    @Column(name = "base_url", nullable = false, length = 512)
    private String baseUrl;

    @Column(nullable = false, length = 80)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ServerStatus status = ServerStatus.ACTIVE;

    @Column(name = "last_sync_at")
    private Instant lastSyncAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected FederatedServer() {
    }

    public FederatedServer(String baseUrl, String name) {
        this.baseUrl = baseUrl;
        this.name = name;
    }

    public void markSynced() {
        this.status = ServerStatus.ACTIVE;
        this.lastSyncAt = Instant.now();
    }

    public void markUnreachable() {
        this.status = ServerStatus.UNREACHABLE;
    }

    public void rename(String name) {
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getName() {
        return name;
    }

    public ServerStatus getStatus() {
        return status;
    }

    public Instant getLastSyncAt() {
        return lastSyncAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
