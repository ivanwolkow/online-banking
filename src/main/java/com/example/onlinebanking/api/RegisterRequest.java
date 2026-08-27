package com.example.onlinebanking.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Details required to create a customer and current account")
public record RegisterRequest(
        @NotBlank @Size(min = 2, max = 100) @Schema(example = "Ada Lovelace") String fullName,
        @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "[A-Za-z0-9._-]+")
        @Schema(example = "ada.lovelace") String username,
        @NotNull @JsonFormat(pattern = "yyyy-MM-dd") @Schema(example = "1990-12-10") LocalDate dateOfBirth,
        @NotNull @Valid AddressRequest address) {
}
