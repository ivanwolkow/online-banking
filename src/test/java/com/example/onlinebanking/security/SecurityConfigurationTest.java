package com.example.onlinebanking.security;

import com.example.onlinebanking.config.AppProperties;
import com.example.onlinebanking.config.ApplicationConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityConfigurationTest {
    private final SecurityConfiguration configuration = new SecurityConfiguration();

    @Test
    void rejectsSigningSecretsShorterThan256Bits() {
        AppProperties properties = properties("dG9vLXNob3J0");

        assertThatThrownBy(() -> configuration.jwtSecretKey(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    @Test
    void applicationConfigurationFailsWithoutSigningMaterial() {
        new ApplicationContextRunner()
                .withUserConfiguration(ApplicationConfiguration.class)
                .withPropertyValues(
                        "app.clock-zone=Europe/Amsterdam",
                        "app.registration.minimum-age=18",
                        "app.registration.allowed-countries[0]=NL",
                        "app.account.iban-country-code=NL",
                        "app.account.iban-bank-code=RBNK",
                        "app.account.type=CURRENT",
                        "app.account.currency=EUR",
                        "app.account.initial-balance=0.00",
                        "app.security.jwt.issuer=online-banking",
                        "app.security.jwt.ttl=15m",
                        "app.database.rate-limit.enabled=true",
                        "app.database.rate-limit.operations-per-second=2"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(rootCause(context.getStartupFailure()).getMessage())
                            .contains("app.security.jwt.secretBase64");
                });
    }

    private static Throwable rootCause(Throwable failure) {
        Throwable result = failure;
        while (result.getCause() != null) {
            result = result.getCause();
        }

        return result;
    }

    private static AppProperties properties(String secretBase64) {
        return new AppProperties(
                "Europe/Amsterdam",
                new AppProperties.Registration(18, List.of("NL", "BE")),
                new AppProperties.Account("NL", "RBNK", "CURRENT", "EUR", BigDecimal.ZERO),
                new AppProperties.Security(
                        new AppProperties.Jwt("online-banking", Duration.ofMinutes(15), secretBase64)
                ),
                new AppProperties.Database(new AppProperties.RateLimit(true, 2))
        );
    }
}
