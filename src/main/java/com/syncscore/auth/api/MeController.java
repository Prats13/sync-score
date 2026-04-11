package com.syncscore.auth.api;

import com.syncscore.auth.service.dto.MeResponse;
import com.syncscore.security.AccessPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class MeController {
    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal AccessPrincipal principal) {
        return new MeResponse(principal.userId(), principal.username(), principal.roles());
    }
}

