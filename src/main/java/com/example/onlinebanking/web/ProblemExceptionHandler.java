package com.example.onlinebanking.web;

import com.example.onlinebanking.exception.AccountNotFoundException;
import com.example.onlinebanking.exception.DomainException;
import com.example.onlinebanking.exception.InvalidCredentialsException;
import com.example.onlinebanking.model.ProblemResponse;
import com.example.onlinebanking.persistence.DatabaseBusyException;
import com.example.onlinebanking.persistence.SchemaConstraints;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@RestControllerAdvice
public class ProblemExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProblemExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemResponse> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<ProblemResponse.FieldErrorResponse> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ProblemResponse.FieldErrorResponse(error.getField(), error.getDefaultMessage()))
                .toList();

        return response(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Request validation failed",
                "One or more fields are invalid.",
                request,
                errors
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemResponse> malformed(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return response(
                HttpStatus.BAD_REQUEST,
                "MALFORMED_REQUEST",
                "Malformed request",
                "The request body could not be read.",
                request,
                null
        );
    }

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ProblemResponse> domain(DomainException exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, exception.code(), exception.title(), exception.getMessage(), request, null);
    }

    @ExceptionHandler(AccountNotFoundException.class)
    ResponseEntity<ProblemResponse> accountNotFound(AccountNotFoundException exception, HttpServletRequest request) {
        return internal(exception, request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ProblemResponse> invalidCredentials(InvalidCredentialsException exception, HttpServletRequest request) {
        return response(
                HttpStatus.UNAUTHORIZED,
                exception.code(),
                exception.title(),
                exception.getMessage(),
                request,
                null
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemResponse> integrity(DataIntegrityViolationException exception, HttpServletRequest request) {
        if (hasConstraint(exception, SchemaConstraints.CUSTOMER_USERNAME_UNIQUE)) {
            return response(
                    HttpStatus.BAD_REQUEST,
                    "USERNAME_ALREADY_EXISTS",
                    "Username already exists",
                    "The username is already in use.",
                    request,
                    null
            );
        }
        // TODO: When metrics are introduced, count ACCOUNT_IBAN_UNIQUE violations.
        return internal(exception, request);
    }

    @ExceptionHandler(DatabaseBusyException.class)
    ResponseEntity<ProblemResponse> busy(DatabaseBusyException exception, HttpServletRequest request) {
        return internal(exception, request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemResponse> unexpected(Exception exception, HttpServletRequest request) {
        return internal(exception, request);
    }

    private ResponseEntity<ProblemResponse> internal(Exception exception, HttpServletRequest request) {
        LOGGER.error("Internal error while handling {}", request.getRequestURI(), exception);

        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "Internal server error",
                "An unexpected error occurred.",
                request,
                null
        );
    }

    private ResponseEntity<ProblemResponse> response(
            HttpStatus status,
            String code,
            String title,
            String detail,
            HttpServletRequest request,
            List<ProblemResponse.FieldErrorResponse> errors
    ) {
        ProblemResponse problem = problem(status, code, title, detail, request, errors);

        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private static boolean hasConstraint(Throwable exception, String expectedName) {
        for (Throwable current = exception; current != null; current = current.getCause()) {
            if (current instanceof ConstraintViolationException violation
                    && expectedName.equalsIgnoreCase(violation.getConstraintName())) {
                return true;
            }
        }

        return false;
    }

    private ProblemResponse problem(
            HttpStatus status,
            String code,
            String title,
            String detail,
            HttpServletRequest request,
            List<ProblemResponse.FieldErrorResponse> errors
    ) {
        String type = "urn:problem:" + code.toLowerCase(Locale.ROOT).replace('_', '-');

        return new ProblemResponse(
                type,
                title,
                status.value(),
                detail,
                request.getRequestURI(),
                code,
                Instant.now(),
                errors
        );
    }
}
