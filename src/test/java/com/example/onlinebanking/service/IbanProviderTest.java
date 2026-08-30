package com.example.onlinebanking.service;

import com.example.onlinebanking.config.AppProperties;
import org.iban4j.IbanUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class IbanProviderTest {
    @Test
    void providesValidDutchAccountNumbersForTheConfiguredBank() {
        IbanProvider provider = new IbanProvider(properties(), new SecureRandom());
        Set<String> values = new HashSet<>();

        for (int index = 0; index < 20; index++) {
            String iban = provider.provide();

            assertThat(iban).matches("^NL[0-9]{2}RBNK[0-9]{10}$");
            assertThat(IbanUtil.isValid(iban)).isTrue();
            values.add(iban);
        }

        assertThat(values).hasSizeGreaterThan(1);
    }

    private static AppProperties properties() {
        return new AppProperties(
                "UTC",
                new AppProperties.Registration(18, List.of("NL", "BE")),
                new AppProperties.Account("NL", "RBNK", "CURRENT", "EUR", BigDecimal.ZERO),
                new AppProperties.Security(new AppProperties.Jwt("online-banking", Duration.ofMinutes(15), "secret")),
                new AppProperties.Database(new AppProperties.RateLimit(true, 2))
        );
    }
}
