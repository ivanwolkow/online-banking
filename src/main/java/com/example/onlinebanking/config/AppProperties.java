package com.example.onlinebanking.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

@Validated
@ConfigurationProperties("app")
public record AppProperties(
        @NotBlank String clockZone,
        @NotNull @Valid Registration registration,
        @NotNull @Valid Account account,
        @NotNull @Valid Security security,
        @NotNull @Valid Database database) {
    public record Registration(
            @Min(1) int minimumAge,
            @NotEmpty List<@NotBlank String> allowedCountries
    ) {
    }

    public record Account(
            @NotBlank String ibanCountryCode,
            @NotBlank String ibanBankCode,
            @NotBlank String type,
            @NotBlank String currency,
            @NotNull BigDecimal initialBalance
    ) {
    }

    public record Security(@NotNull @Valid Jwt jwt) {
    }

    public record Jwt(@NotBlank String issuer, @NotNull Duration ttl, @NotBlank String secretBase64) {
        public Jwt {
            if (ttl != null && (ttl.isNegative() || ttl.isZero())) {
                throw new IllegalArgumentException("JWT TTL must be positive");
            }
        }
    }
    public record Database(@NotNull @Valid RateLimit rateLimit) {
    }

    public record RateLimit(boolean enabled, @Positive int operationsPerSecond) {
    }
}
