package com.example.onlinebanking.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Credential returned once immediately after registration")
public record RegisterResponse(
        @Schema(example = "ada.lovelace") String username,
        @Schema(example = "c29tZVJhbmRvbV8x") String defaultPassword) {
}
