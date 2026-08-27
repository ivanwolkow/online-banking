package com.example.onlinebanking.domain;

import java.security.SecureRandom;
import java.util.Base64;

public class SecurePasswordGenerator implements PasswordGenerator {
    private final SecureRandom random;

    public SecurePasswordGenerator(SecureRandom random) {
        this.random = random;
    }

    @Override
    public String generate() {
        byte[] bytes = new byte[16]; // 128 bits of entropy
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
