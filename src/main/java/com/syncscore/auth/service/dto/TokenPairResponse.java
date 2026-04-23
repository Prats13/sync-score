package com.syncscore.auth.service.dto;

public record TokenPairResponse(
        String accessToken,
        String refreshToken,
        String tokenType
) {}

