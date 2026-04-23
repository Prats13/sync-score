package com.syncscore.security.google;

public interface GoogleTokenVerifier {
    GoogleIdTokenPayload verify(String idToken);
}

