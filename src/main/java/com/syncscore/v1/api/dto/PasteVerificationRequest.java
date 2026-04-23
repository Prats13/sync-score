package com.syncscore.v1.api.dto;

import jakarta.validation.constraints.NotBlank;

public record PasteVerificationRequest(
        @NotBlank String content,
        // Optional hint: "package_json" or "requirements_txt". If absent the orchestrator
        // infers the type from the content (JSON-looking → package.json, else requirements.txt).
        String manifestType
) {}
