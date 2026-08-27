package com.example.onlinebanking.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Username and generated default password")
public record LoginRequest(
        @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "[A-Za-z0-9._-]+")
        @Schema(example = "ada.lovelace") String username,
        @NotBlank @Size(max = 100) @Schema(example = "c29tZVJhbmRvbV8x") String password) {
}
