package com.syncscore.auth.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ResendOtpSender implements OtpSender {
    private static final String RESEND_URL = "https://api.resend.com/emails";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String apiKey;
    private final String from;

    public ResendOtpSender(String apiKey, String from) {
        this.apiKey = apiKey;
        this.from = from;
    }

    @Override
    public void sendSignupOtp(String email, String otp) {
        String body = """
                {"from":"%s","to":["%s"],"subject":"Your SyncScore verification code","text":"Your verification code is: %s\\n\\nThis code expires in 10 minutes."}
                """.formatted(from, email, otp);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RESEND_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to send verification email. Please try again later.");
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to send verification email. Please try again later.");
        }
    }
}