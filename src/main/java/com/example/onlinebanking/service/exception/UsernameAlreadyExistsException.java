package com.example.onlinebanking.service.exception;

public final class UsernameAlreadyExistsException extends DomainException {

    public UsernameAlreadyExistsException() {
        super("USERNAME_ALREADY_EXISTS", "Username already exists", "The username is already in use.");
    }
}
