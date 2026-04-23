package com.syncscore.producer.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ProducerProfileRequest(
        @NotBlank
        @Pattern(regexp = "https?://.+", message = "must be a valid URL")
        String linkedinUrl,

        @Pattern(regexp = "https?://.+", message = "must be a valid URL")
        String githubUrl,

        @NotBlank
        @Pattern(regexp = "https?://.+", message = "must be a valid URL")
        String websiteUrl,

        @NotBlank
        @Pattern(regexp = "https?://.+", message = "must be a valid URL")
        String liveProjectUrl
) {}