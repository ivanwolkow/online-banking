package com.example.onlinebanking.service.exception;

public final class AccountNotFoundException extends DomainException {

    public AccountNotFoundException() {
        super("ACCOUNT_NOT_FOUND", "Account not found", "No account exists for this customer.");
    }
}
