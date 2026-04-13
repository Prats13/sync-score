package com.syncscore.producer.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UrlVerificationService {

    private static final Logger log = LoggerFactory.getLogger(UrlVerificationService.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(7);
    private static final String USER_AGENT = "SyncScore-Verifier/1.0";

    private final HttpClient httpClient;

    public UrlVerificationService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public boolean isReachable(String url) {
        try {
            // Try HEAD first (lighter — no body transfer)
            HttpRequest headRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .timeout(TIMEOUT)
                    .header("User-Agent", USER_AGENT)
                    .build();
            HttpResponse<Void> response = httpClient.send(headRequest, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() < 400) {
                return true;
            }
        } catch (Exception e) {
            log.debug("HEAD check failed for {}: {}", url, e.getMessage());
        }

        // Fall back to GET (some servers reject HEAD)
        try {
            HttpRequest getRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(TIMEOUT)
                    .header("User-Agent", USER_AGENT)
                    .build();
            HttpResponse<Void> response = httpClient.send(getRequest, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() < 400;
        } catch (Exception e) {
            log.debug("GET check failed for {}: {}", url, e.getMessage());
            return false;
        }
    }
}