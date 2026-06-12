package com.DSM.Platform.websocket;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Initial presence snapshot for clients that just loaded (updates arrive via /topic/presence). */
@RestController
@RequestMapping("/api/v1/presence")
public class PresenceController {

    private final PresenceService presenceService;

    public PresenceController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @GetMapping
    public PresenceService.PresenceSnapshot snapshot() {
        return presenceService.snapshot();
    }
}
