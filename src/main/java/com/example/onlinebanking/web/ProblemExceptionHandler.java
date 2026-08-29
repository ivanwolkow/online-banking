package com.example.onlinebanking.web;

import com.example.onlinebanking.api.ProblemResponse;
import com.example.onlinebanking.exception.AccountNotFoundException;
import com.example.onlinebanking.exception.AccountNumberGenerationFailedException;
import com.example.onlinebanking.exception.DomainException;
import com.example.onlinebanking.exception.InvalidCredentialsException;
import com.example.onlinebanking.exception.UsernameAlreadyExistsException;
import com.example.onlinebanking.persistence.DatabaseBusyException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
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
        HttpStatus status = statusFor(exception);

        return response(status, exception.code(), exception.title(), exception.getMessage(), request, null);
    }

    @ExceptionHandler(DatabaseBusyException.class)
    ResponseEntity<ProblemResponse> busy(DatabaseBusyException exception, HttpServletRequest request) {
        ProblemResponse problem = problem(
                HttpStatus.SERVICE_UNAVAILABLE,
                "DATABASE_BUSY",
                "Database busy",
                "Please retry shortly.",
                request,
                null
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, "1")
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemResponse> unexpected(Exception exception, HttpServletRequest request) {
        LOGGER.error("Unexpected error while handling {}", request.getRequestURI(), exception);

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

    private HttpStatus statusFor(DomainException exception) {
        if (exception instanceof UsernameAlreadyExistsException) {
            return HttpStatus.CONFLICT;
        }
        if (exception instanceof AccountNotFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        if (exception instanceof InvalidCredentialsException) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (exception instanceof AccountNumberGenerationFailedException) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.BAD_REQUEST;
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
