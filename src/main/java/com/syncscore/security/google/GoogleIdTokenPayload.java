package com.syncscore.security.google;

public record GoogleIdTokenPayload(
        String subject,
        String email
) {}

