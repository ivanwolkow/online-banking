package com.example.onlinebanking.service;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

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

}
