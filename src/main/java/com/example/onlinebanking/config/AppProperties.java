package com.example.onlinebanking.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
        @Valid Registration registration,
        @Valid Account account,
        @Valid Security security,
        @Valid Database database) {
    public record Registration(@Min(1) int minimumAge, @NotEmpty List<@NotBlank String> allowedCountries) {
    }
    public record Account(@NotBlank String ibanCountryCode, @NotBlank String ibanBankCode, @NotBlank String type,
                          @NotBlank String currency, BigDecimal initialBalance) {
    }
    public record Security(@Valid Jwt jwt) {
    }
    public record Jwt(@NotBlank String issuer, @Positive Duration ttl, @NotBlank String secretBase64) {
    }
    public record Database(@Valid RateLimit rateLimit) {
    }
    public record RateLimit(boolean enabled, @Positive int operationsPerSecond, @Positive Duration maxWait) {
    }
}
