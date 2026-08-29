package com.example.onlinebanking.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

@Schema(description = "Current account overview")
public record OverviewResponse(
        @Pattern(regexp = "NL[0-9]{2}[A-Z]{4}[0-9]{10}") @Schema(example = "NL91RBNK0123456789") String accountNumber,
        @Schema(allowableValues = "CURRENT", example = "CURRENT") String accountType,
        @Schema(example = "0.00") BigDecimal balance,
        @Schema(example = "EUR") String currency) {
}
