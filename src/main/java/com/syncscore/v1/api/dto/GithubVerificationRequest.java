package com.syncscore.v1.api.dto;

import jakarta.validation.constraints.NotBlank;

public record GithubVerificationRequest(
        @NotBlank String githubUsername
) {}
