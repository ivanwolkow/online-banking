package com.example.onlinebanking.api;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class RequestNormalizationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void trimsRegistrationStringsBeforeValidation() {
        RegisterRequest request = new RegisterRequest(
                "  Ada Lovelace  ",
                "  ab  ",
                LocalDate.of(1990, 12, 10),
                new AddressRequest(
                        "  Keizersgracht  ",
                        "  123A  ",
                        "  1015 CJ  ",
                        "  Amsterdam  ",
                        "  nl  "
                )
        );

        assertThat(request.fullName()).isEqualTo("Ada Lovelace");
        assertThat(request.username()).isEqualTo("ab");
        assertThat(request.address().street()).isEqualTo("Keizersgracht");
        assertThat(request.address().countryCode()).isEqualTo("nl");
        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("username");
    }

    @Test
    void trimsTheLoginUsernameButLeavesThePasswordUntouched() {
        LoginRequest request = new LoginRequest("  Ada.Lovelace  ", "  generated-password  ");

        assertThat(request.username()).isEqualTo("Ada.Lovelace");
        assertThat(request.password()).isEqualTo("  generated-password  ");
    }
}
