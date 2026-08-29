package com.example.onlinebanking.service;

import org.iban4j.IbanUtil;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RegistrationServiceRulesTest {
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-08-27T10:00:00Z"),
            ZoneId.of("Europe/Amsterdam")
    );

    @Test
    void ageBoundaryUsesTheBusinessDate() {
        assertThat(RegistrationService.isUnderage(LocalDate.of(2008, 8, 28), 18, clock)).isTrue();
        assertThat(RegistrationService.isUnderage(LocalDate.of(2008, 8, 27), 18, clock)).isFalse();
        assertThat(RegistrationService.isUnderage(LocalDate.of(2008, 8, 26), 18, clock)).isFalse();
    }

    @Test
    void generatedPasswordUsesUrlSafeEncodingFor128RandomBits() {
        String password = RegistrationService.generatePassword(new SecureRandom());

        assertThat(password).hasSize(22).matches("[A-Za-z0-9_-]+$");
    }

    @Test
    void iban4jGeneratesValidDutchAccountNumbersForTheConfiguredBank() {
        SecureRandom random = new SecureRandom();
        Set<String> values = new HashSet<>();

        for (int index = 0; index < 20; index++) {
            String iban = RegistrationService.generateIban("NL", "RBNK", random);

            assertThat(iban).matches("^NL[0-9]{2}RBNK[0-9]{10}$");
            assertThat(IbanUtil.isValid(iban)).isTrue();
            values.add(iban);
        }

        assertThat(values).hasSizeGreaterThan(1);
    }
}
