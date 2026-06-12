package com.DSM.Platform.federation;

import com.DSM.Platform.federation.dto.AddServerRequest;
import com.DSM.Platform.federation.dto.RemotePostResponse;
import com.DSM.Platform.federation.dto.ServerResponse;
import com.DSM.Platform.federation.dto.SyncResultResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Authenticated management of this node's federation peers. */
@RestController
@RequestMapping("/api/v1/federation")
public class FederationAdminController {

    private final FederationServerService serverService;
    private final FederationSyncService syncService;

    public FederationAdminController(
            FederationServerService serverService,
            FederationSyncService syncService
    ) {
        this.serverService = serverService;
        this.syncService = syncService;
    }

    @GetMapping("/servers")
    public List<ServerResponse> listServers() {
        return serverService.listServers();
    }

    @PostMapping("/servers")
    @ResponseStatus(HttpStatus.CREATED)
    public ServerResponse addServer(@Valid @RequestBody AddServerRequest request) {
        return serverService.addServer(request);
    }

    @DeleteMapping("/servers/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeServer(@PathVariable UUID id) {
        serverService.removeServer(id);
    }

    @PostMapping("/sync")
    public List<SyncResultResponse> syncNow() {
        return syncService.syncAll();
    }

    @GetMapping("/timeline")
    public Page<RemotePostResponse> timeline(@PageableDefault(size = 20) Pageable pageable) {
        return serverService.getTimeline(pageable);
    }
}
