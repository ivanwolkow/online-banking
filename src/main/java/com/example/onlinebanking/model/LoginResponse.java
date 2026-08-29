package com.example.onlinebanking.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Signed bearer token")
public record LoginResponse(
        @Schema(example = "eyJ...") String accessToken,
        @Schema(example = "Bearer") String tokenType,
        @Schema(example = "900") long expiresIn) {
}
