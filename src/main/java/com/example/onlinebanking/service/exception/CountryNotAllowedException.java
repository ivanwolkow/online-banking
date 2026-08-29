package com.example.onlinebanking.service.exception;

public final class CountryNotAllowedException extends DomainException {

    public CountryNotAllowedException() {
        super("COUNTRY_NOT_ALLOWED", "Country not allowed", "The address country is not allowed.");
    }
}
