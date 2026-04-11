package com.syncscore.auth.service;

import com.syncscore.auth.service.dto.TokenPairResponse;
import com.syncscore.auth.service.dto.TokenResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class AuthServiceExceptions {
    private AuthServiceExceptions() {}

    static ResponseStatusException emailAlreadyRegistered() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
    }

    static ResponseStatusException usernameTaken() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
    }

    static ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    static ResponseStatusException disabled() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Account disabled");
    }

    static ResponseStatusException alreadyActive() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Account already active");
    }

    static ResponseStatusException userNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
    }

    static ResponseStatusException invalidRefreshToken() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
    }

    static SignupIncompleteException signupIncomplete(String signupToken) {
        return new SignupIncompleteException(signupToken);
    }

    public static final class SignupIncompleteException extends RuntimeException {
        private final String signupToken;

        SignupIncompleteException(String signupToken) {
            super("Signup incomplete");
            this.signupToken = signupToken;
        }

        public String getSignupToken() {
            return signupToken;
        }
    }
}
