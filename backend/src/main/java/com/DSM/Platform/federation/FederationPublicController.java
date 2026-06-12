package com.DSM.Platform.federation;

import com.DSM.Platform.federation.dto.AnnounceRequest;
import com.DSM.Platform.federation.dto.FederationInfoDto;
import com.DSM.Platform.federation.dto.FederationPostDto;
import com.DSM.Platform.federation.dto.FederationUserDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Server-to-server federation protocol. Intentionally unauthenticated: it
 * only exposes public data, and announce only registers a peer URL (an
 * accepted MVP spam risk — a shared secret can be layered on later).
 */
@RestController
@RequestMapping("/federation")
public class FederationPublicController {

    private final FederationPublicService publicService;
    private final FederationServerService serverService;

    public FederationPublicController(
            FederationPublicService publicService,
            FederationServerService serverService
    ) {
        this.publicService = publicService;
        this.serverService = serverService;
    }

    @GetMapping("/posts")
    public Page<FederationPostDto> posts(@PageableDefault(size = 20) Pageable pageable) {
        return publicService.getPublicPosts(pageable);
    }

    @GetMapping("/users")
    public Page<FederationUserDto> users(@PageableDefault(size = 20) Pageable pageable) {
        return publicService.getPublicUsers(pageable);
    }

    @GetMapping("/info")
    public FederationInfoDto info() {
        return publicService.getInfo();
    }

    @PostMapping("/announce")
    public FederationInfoDto announce(@Valid @RequestBody AnnounceRequest request) {
        serverService.registerAnnouncedPeer(request);
        // Reply with our identity so the announcer can confirm who it reached.
        return publicService.getInfo();
    }
}
