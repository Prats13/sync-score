package com.syncscore.v2.api.dto;

import java.util.List;

public record ScanDetailResponse(
        ArchitectureScanResponse scan,
        List<ScanSignalResponse> signals,
        List<ScanRepoResponse> repos
) {}
