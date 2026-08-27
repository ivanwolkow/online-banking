package com.example.onlinebanking.domain;

import java.time.Clock;
import java.time.LocalDate;

public final class AgeEligibility {
    private AgeEligibility() {
    }

    public static boolean isEligible(LocalDate dateOfBirth, int minimumAge, Clock clock) {
        return !dateOfBirth.plusYears(minimumAge).isAfter(LocalDate.now(clock));
    }
}
