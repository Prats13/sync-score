package com.syncscore.security.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class GoogleTokenVerifierImpl implements GoogleTokenVerifier {
    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifierImpl(@Value("${app.google.client-id:}") String clientId) {
        if (clientId == null || clientId.isBlank()) {
            this.verifier = null;
            return;
        }
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), JacksonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    @Override
    public GoogleIdTokenPayload verify(String idToken) {
        if (verifier == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Google sign-in not configured");
        }
        try {
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google token");
            }
            GoogleIdToken.Payload payload = token.getPayload();
            Object emailVerified = payload.get("email_verified");
            if (emailVerified instanceof Boolean ev && !ev) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google email not verified");
            }
            return new GoogleIdTokenPayload(payload.getSubject(), payload.getEmail());
        } catch (GeneralSecurityException | IOException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google token");
        }
    }
}

