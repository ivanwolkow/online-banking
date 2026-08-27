package com.example.onlinebanking.domain;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DomainRulesTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-27T10:00:00Z"), ZoneId.of("Europe/Amsterdam"));

    @Test
    void ageBoundaryUsesTheBusinessDate() {
        assertThat(AgeEligibility.isEligible(LocalDate.of(2008, 8, 28), 18, clock)).isFalse();
        assertThat(AgeEligibility.isEligible(LocalDate.of(2008, 8, 27), 18, clock)).isTrue();
        assertThat(AgeEligibility.isEligible(LocalDate.of(2008, 8, 26), 18, clock)).isTrue();
    }

    @Test
    void normalizesUsernameAndCreatesSecurePasswords() {
        assertThat(UsernameNormalizer.normalize(" Ada.Lovelace ")).isEqualTo("ada.lovelace");
        String password = new SecurePasswordGenerator(new SecureRandom()).generate();
        assertThat(password).hasSize(22).matches("[A-Za-z0-9_-]+$");
    }

    @Test
    void generatedIbansAreDutchAndChecksumValid() {
        IbanGenerator generator = new IbanGenerator("NL", "RBNK", new SecureRandom());
        Set<String> values = new HashSet<>();
        for (int index = 0; index < 20; index++) {
            String iban = generator.generate();
            assertThat(iban).matches("^NL[0-9]{2}[A-Z]{4}[0-9]{10}$");
            assertThat(IbanGenerator.isValid(iban)).isTrue();
            values.add(iban);
        }
        assertThat(values).hasSizeGreaterThan(1);
    }
}
