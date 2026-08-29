package com.example.onlinebanking.web;

import com.example.onlinebanking.model.ProblemResponse;
import com.example.onlinebanking.exception.AccountNotFoundException;
import com.example.onlinebanking.exception.AccountNumberGenerationFailedException;
import com.example.onlinebanking.persistence.DatabaseBusyException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemExceptionHandlerTest {
    private final ProblemExceptionHandler handler = new ProblemExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/register");

    @Test
    void hidesAccountNumberGenerationDetailsFromTheClient() {
        assertInternal(handler.domain(new AccountNumberGenerationFailedException(), request));
    }

    @Test
    void hidesDatabaseCapacityDetailsFromTheClient() {
        assertInternal(handler.busy(new DatabaseBusyException(), request));
    }

    @Test
    void hidesMissingAccountDetailsFromTheClient() {
        assertInternal(handler.domain(new AccountNotFoundException(), request));
    }

    private static void assertInternal(ResponseEntity<ProblemResponse> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody())
                .extracting(ProblemResponse::code, ProblemResponse::title, ProblemResponse::detail)
                .containsExactly("INTERNAL_ERROR", "Internal server error", "An unexpected error occurred.");
    }
}
