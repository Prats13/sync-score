package com.syncscore.web;

import com.syncscore.auth.service.AuthServiceExceptions;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> handleStatus(ResponseStatusException e) {
        HttpStatus status = (HttpStatus) e.getStatusCode();
        return ResponseEntity.status(status).body(Map.of("error", e.getReason()));
    }

    @ExceptionHandler(AuthServiceExceptions.SignupIncompleteException.class)
    public ResponseEntity<?> handleSignupIncomplete(AuthServiceExceptions.SignupIncompleteException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "Signup incomplete",
                "signupToken", e.getSignupToken()
        ));
    }
}

