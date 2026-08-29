package com.example.onlinebanking.service.exception;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DomainExceptionTest {

    @Test
    void concreteExceptionsOwnTheirProblemMetadata() {
        List<DomainException> exceptions = List.of(
                new CustomerUnderageException(),
                new CountryNotAllowedException(),
                new UsernameAlreadyExistsException(),
                new AccountNumberGenerationFailedException(),
                new InvalidCredentialsException(),
                new AccountNotFoundException()
        );

        assertThat(exceptions)
                .extracting(DomainException::code)
                .containsExactly(
                        "CUSTOMER_UNDERAGE",
                        "COUNTRY_NOT_ALLOWED",
                        "USERNAME_ALREADY_EXISTS",
                        "ACCOUNT_NUMBER_GENERATION_FAILED",
                        "INVALID_CREDENTIALS",
                        "ACCOUNT_NOT_FOUND"
                );
        assertThat(exceptions).allSatisfy(exception -> {
            assertThat(exception.title()).isNotBlank();
            assertThat(exception.getMessage()).isNotBlank();
        });
    }
}
