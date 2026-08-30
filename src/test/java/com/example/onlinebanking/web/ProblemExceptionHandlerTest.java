package com.example.onlinebanking.web;

import com.example.onlinebanking.model.ProblemResponse;
import com.example.onlinebanking.exception.AccountNotFoundException;
import com.example.onlinebanking.exception.InvalidCredentialsException;
import com.example.onlinebanking.persistence.DatabaseBusyException;
import com.example.onlinebanking.persistence.SchemaConstraints;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemExceptionHandlerTest {
    private final ProblemExceptionHandler handler = new ProblemExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/register");

    @Test
    void hidesDatabaseCapacityDetailsFromTheClient() {
        assertInternal(handler.busy(new DatabaseBusyException(), request));
    }

    @Test
    void hidesMissingAccountDetailsFromTheClient() {
        assertInternal(handler.accountNotFound(new AccountNotFoundException(), request));
    }

    @Test
    void mapsTheExplicitUsernameConstraintToACorrectableProblem() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "Duplicate username",
                new ConstraintViolationException(
                        "Duplicate username",
                        new SQLException(),
                        SchemaConstraints.CUSTOMER_USERNAME_UNIQUE
                )
        );

        ResponseEntity<ProblemResponse> response = handler.integrity(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .extracting(ProblemResponse::code, ProblemResponse::title, ProblemResponse::detail)
                .containsExactly("USERNAME_ALREADY_EXISTS", "Username already exists", "The username is already in use.");
    }

    @Test
    void mapsInvalidCredentialsToUnauthorized() {
        ResponseEntity<ProblemResponse> response = handler.invalidCredentials(new InvalidCredentialsException(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody())
                .extracting(ProblemResponse::code)
                .isEqualTo("INVALID_CREDENTIALS");
    }

    private static void assertInternal(ResponseEntity<ProblemResponse> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody())
                .extracting(ProblemResponse::code, ProblemResponse::title, ProblemResponse::detail)
                .containsExactly("INTERNAL_ERROR", "Internal server error", "An unexpected error occurred.");
    }
}
