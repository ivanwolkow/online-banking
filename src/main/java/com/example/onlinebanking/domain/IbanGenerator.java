package com.example.onlinebanking.domain;

import java.security.SecureRandom;
import java.util.Locale;

public class IbanGenerator {
    private final String countryCode;
    private final String bankCode;
    private final SecureRandom random;

    public IbanGenerator(String countryCode, String bankCode, SecureRandom random) {
        this.countryCode = countryCode.toUpperCase(Locale.ROOT);
        this.bankCode = bankCode.toUpperCase(Locale.ROOT);
        this.random = random;
    }

    public String generate() {
        long account = random.nextLong(1, 10_000_000_000L);
        String bban = bankCode + String.format(Locale.ROOT, "%010d", account);
        int checkDigits = 98 - mod97(bban + countryCode + "00");
        return countryCode + String.format(Locale.ROOT, "%02d", checkDigits) + bban;
    }

    public static boolean isValid(String iban) {
        if (iban == null || !iban.matches("^NL[0-9]{2}[A-Z]{4}[0-9]{10}$")) {
            return false;
        }
        return mod97(iban.substring(4) + iban.substring(0, 4)) == 1;
    }

    private static int mod97(String value) {
        int remainder = 0;
        for (char character : value.toCharArray()) {
            if (Character.isDigit(character)) {
                remainder = (remainder * 10 + (character - '0')) % 97;
            } else {
                int numeric = character - 'A' + 10;
                remainder = (remainder * 10 + numeric / 10) % 97;
                remainder = (remainder * 10 + numeric % 10) % 97;
            }
        }
        return remainder;
    }
}
