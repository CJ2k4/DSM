package com.DSM.Platform.federation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.federation")
public class FederationProperties {

    /** Public base URL other nodes use to reach this server. */
    @NotBlank
    private String selfBaseUrl = "http://localhost:8080";

    /** Human-readable name announced to peers. */
    @NotBlank
    private String serverName = "DSM Node";

    /** When true, a background scheduler pulls posts from peers periodically. */
    private boolean autoSync = true;

    @NotNull
    private Duration syncInterval = Duration.ofSeconds(60);

    /** Upper bound on posts pulled from one peer during a single sync. */
    private int maxPostsPerSync = 200;

    @NotNull
    private Duration connectTimeout = Duration.ofSeconds(3);

    @NotNull
    private Duration readTimeout = Duration.ofSeconds(5);

    public String getSelfBaseUrl() {
        return selfBaseUrl;
    }

    public void setSelfBaseUrl(String selfBaseUrl) {
        this.selfBaseUrl = selfBaseUrl;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public boolean isAutoSync() {
        return autoSync;
    }

    public void setAutoSync(boolean autoSync) {
        this.autoSync = autoSync;
    }

    public Duration getSyncInterval() {
        return syncInterval;
    }

    public void setSyncInterval(Duration syncInterval) {
        this.syncInterval = syncInterval;
    }

    public int getMaxPostsPerSync() {
        return maxPostsPerSync;
    }

    public void setMaxPostsPerSync(int maxPostsPerSync) {
        this.maxPostsPerSync = maxPostsPerSync;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }
}
