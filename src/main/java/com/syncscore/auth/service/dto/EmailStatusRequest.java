package com.syncscore.auth.service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailStatusRequest(
        @NotBlank @Email String email
) {}

