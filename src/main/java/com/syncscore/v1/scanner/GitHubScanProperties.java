package com.syncscore.v1.scanner;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.github")
public record GitHubScanProperties(
        String apiBaseUrl,
        String token,
        int repoScanDefaultLimit,
        int repoScanMaxLimit,
        int listReposPerPage
) {
    public GitHubScanProperties {
        if (apiBaseUrl == null || apiBaseUrl.isBlank()) {
            apiBaseUrl = "https://api.github.com";
        }
        if (repoScanDefaultLimit <= 0) {
            repoScanDefaultLimit = 10;
        }
        if (repoScanMaxLimit <= 0) {
            repoScanMaxLimit = 15;
        }
        if (listReposPerPage <= 0 || listReposPerPage > 100) {
            listReposPerPage = 100;
        }
    }
}

