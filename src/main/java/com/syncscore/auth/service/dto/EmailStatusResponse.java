package com.syncscore.auth.service.dto;

public record EmailStatusResponse(
        boolean exists,
        String status
) {}

