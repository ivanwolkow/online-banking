package com.example.onlinebanking.web;

import com.example.onlinebanking.api.ProblemResponse;
import com.example.onlinebanking.persistence.DatabaseBusyException;
import com.example.onlinebanking.service.DomainException;
import jakarta.servlet.http.HttpServletRequest;
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

@RestControllerAdvice
public class ProblemExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemResponse> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<ProblemResponse.FieldErrorResponse> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ProblemResponse.FieldErrorResponse(error.getField(), error.getDefaultMessage())).toList();
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", "One or more fields are invalid.", request, errors);
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemResponse> malformed(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Malformed request", "The request body could not be read.", request, null);
    }
    @ExceptionHandler(DomainException.class)
    ResponseEntity<ProblemResponse> domain(DomainException exception, HttpServletRequest request) {
        HttpStatus status = switch (exception.code()) {
            case "USERNAME_ALREADY_EXISTS" -> HttpStatus.CONFLICT;
            case "ACCOUNT_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "INVALID_CREDENTIALS" -> HttpStatus.UNAUTHORIZED;
            case "ACCOUNT_NUMBER_GENERATION_FAILED" -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST;
        };
        return response(status, exception.code(), status == HttpStatus.UNAUTHORIZED ? "Authentication failed" : "Request failed", exception.getMessage(), request, null);
    }
    @ExceptionHandler(DatabaseBusyException.class)
    ResponseEntity<ProblemResponse> busy(DatabaseBusyException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).header(HttpHeaders.RETRY_AFTER, "1")
                .contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problem(HttpStatus.SERVICE_UNAVAILABLE, "DATABASE_BUSY", "Database busy", "Please retry shortly.", request, null));
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemResponse> unexpected(Exception exception, HttpServletRequest request) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Internal server error", "An unexpected error occurred.", request, null);
    }
    private ResponseEntity<ProblemResponse> response(HttpStatus status, String code, String title, String detail, HttpServletRequest request, List<ProblemResponse.FieldErrorResponse> errors) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problem(status, code, title, detail, request, errors));
    }
    private ProblemResponse problem(HttpStatus status, String code, String title, String detail, HttpServletRequest request, List<ProblemResponse.FieldErrorResponse> errors) {
        return new ProblemResponse("urn:problem:" + code.toLowerCase().replace('_', '-'), title, status.value(), detail, request.getRequestURI(), code, Instant.now(), errors);
    }
}
