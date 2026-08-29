package com.example.onlinebanking.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "RFC 9457-style application problem")
public record ProblemResponse(
        @Schema(description = "Problem type URI") String type,
        @Schema(description = "Short summary of the problem") String title,
        @Schema(description = "Always matches the HTTP response status") int status,
        @Schema(description = "Human-readable explanation") String detail,
        @Schema(description = "Request URI that produced the problem") String instance,
        @Schema(description = "Stable application error code") String code,
        @Schema(description = "Time at which the problem was produced") Instant timestamp,
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<FieldErrorResponse> errors) {

    public record FieldErrorResponse(String field, String message) {
    }
}
