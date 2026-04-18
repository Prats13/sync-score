package com.syncscore.v2.api.dto;

public record ReviewCaseResolveRequest(
        String action,   // "APPROVE" or "DISMISS"
        String note
) {}
