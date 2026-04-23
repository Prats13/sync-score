package com.syncscore.observability;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class GitHubApiHealthIndicator implements HealthIndicator {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    @Override
    public Health health() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com"))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();

            HttpResponse<Void> resp = httpClient.send(req, HttpResponse.BodyHandlers.discarding());
            if (resp.statusCode() == 200) {
                return Health.up().withDetail("provider", "github").build();
            }
            return Health.down()
                    .withDetail("provider", "github")
                    .withDetail("statusCode", resp.statusCode())
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("provider", "github")
                    .withDetail("error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())
                    .build();
        }
    }
}

