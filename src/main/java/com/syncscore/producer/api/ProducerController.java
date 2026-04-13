package com.syncscore.producer.api;

import com.syncscore.producer.service.ProducerService;
import com.syncscore.producer.service.dto.ProducerProfileRequest;
import com.syncscore.producer.service.dto.ProducerProfileResponse;
import com.syncscore.security.AccessPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/producer")
public class ProducerController {

    private final ProducerService producerService;

    public ProducerController(ProducerService producerService) {
        this.producerService = producerService;
    }

    @PostMapping("/profile")
    public ProducerProfileResponse submitProfile(
            @AuthenticationPrincipal AccessPrincipal principal,
            @Valid @RequestBody ProducerProfileRequest request) {
        return producerService.submitProfile(principal.userId(), request);
    }

    @GetMapping("/profile")
    public ProducerProfileResponse getProfile(
            @AuthenticationPrincipal AccessPrincipal principal) {
        return producerService.getProfile(principal.userId());
    }

    @PostMapping("/profile/verify")
    public ProducerProfileResponse verifyProfile(
            @AuthenticationPrincipal AccessPrincipal principal) {
        return producerService.verifyProfile(principal.userId());
    }
}