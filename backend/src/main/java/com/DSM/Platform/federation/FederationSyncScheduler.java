package com.DSM.Platform.federation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Periodic background pull from all registered peers. Kept separate from the
 * service (and conditional) so tests and single-node setups can switch it off
 * with app.federation.auto-sync=false.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "app.federation", name = "auto-sync", havingValue = "true", matchIfMissing = true)
public class FederationSyncScheduler {

    private final FederationSyncService syncService;

    public FederationSyncScheduler(FederationSyncService syncService) {
        this.syncService = syncService;
    }

    @Scheduled(
            fixedDelayString = "${app.federation.sync-interval:PT60S}",
            initialDelayString = "${app.federation.sync-interval:PT60S}"
    )
    public void autoSync() {
        syncService.syncAll();
    }
}
