package com.example.onlinebanking.service.exception;

public final class AccountNumberGenerationFailedException extends DomainException {

    public AccountNumberGenerationFailedException() {
        super(
                "ACCOUNT_NUMBER_GENERATION_FAILED",
                "Account number generation failed",
                "Could not create a unique account number."
        );
    }
}
