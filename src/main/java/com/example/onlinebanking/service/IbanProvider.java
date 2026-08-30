package com.example.onlinebanking.service;

import com.example.onlinebanking.config.AppProperties;
import org.iban4j.CountryCode;
import org.iban4j.Iban;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Locale;

@Service
public class IbanProvider {
    private final AppProperties properties;
    private final SecureRandom random;

    public IbanProvider(AppProperties properties, SecureRandom random) {
        this.properties = properties;
        this.random = random;
    }

    public String provide() {
        return new Iban.Builder(random)
                .countryCode(CountryCode.getByCode(properties.account().ibanCountryCode().toUpperCase(Locale.ROOT)))
                .bankCode(properties.account().ibanBankCode().toUpperCase(Locale.ROOT))
                .buildRandom()
                .toString();
    }
}
