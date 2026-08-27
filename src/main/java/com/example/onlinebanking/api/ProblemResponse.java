package com.example.onlinebanking.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "RFC 9457-style application problem")
public record ProblemResponse(
        @Schema(example = "urn:problem:validation-error") String type,
        @Schema(example = "Request validation failed") String title,
        @Schema(example = "400") int status,
        @Schema(example = "One or more fields are invalid.") String detail,
        @Schema(example = "/register") String instance,
        @Schema(example = "VALIDATION_ERROR") String code,
        @Schema(example = "2026-08-27T12:00:00Z") Instant timestamp,
        List<FieldErrorResponse> errors) {

    public record FieldErrorResponse(String field, String message) {
    }
}
