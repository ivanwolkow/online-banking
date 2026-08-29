package com.example.onlinebanking.service.exception;

public final class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super("INVALID_CREDENTIALS", "Authentication failed", "The username or password is incorrect.");
    }
}
