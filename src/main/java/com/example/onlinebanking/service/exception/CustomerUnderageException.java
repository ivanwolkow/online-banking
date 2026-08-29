package com.example.onlinebanking.service.exception;

public final class CustomerUnderageException extends DomainException {

    public CustomerUnderageException() {
        super("CUSTOMER_UNDERAGE", "Customer is underage", "Customer has not reached the minimum age.");
    }
}
