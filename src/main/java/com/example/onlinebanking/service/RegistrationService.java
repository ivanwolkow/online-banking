package com.example.onlinebanking.service;

import com.example.onlinebanking.config.AppProperties;
import com.example.onlinebanking.exception.CountryNotAllowedException;
import com.example.onlinebanking.exception.CustomerUnderageException;
import com.example.onlinebanking.model.AddressRequest;
import com.example.onlinebanking.model.RegisterRequest;
import com.example.onlinebanking.model.RegisterResponse;
import com.example.onlinebanking.persistence.*;
import org.iban4j.CountryCode;
import org.iban4j.Iban;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

@Service
public class RegistrationService {
    private final CustomerRepository customers;
    private final AccountRepository accounts;
    private final AppProperties properties;
    private final Clock clock;
    private final PasswordEncoder encoder;
    private final SecureRandom random;
    private final DatabaseOperationGate databaseOperations;

    public RegistrationService(
            CustomerRepository customers,
            AccountRepository accounts,
            AppProperties properties,
            Clock clock,
            PasswordEncoder encoder,
            SecureRandom random,
            DatabaseOperationGate databaseOperations
    ) {
        this.customers = customers;
        this.accounts = accounts;
        this.properties = properties;
        this.clock = clock;
        this.encoder = encoder;
        this.random = random;
        this.databaseOperations = databaseOperations;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        String country = request.address().countryCode().toUpperCase(Locale.ROOT);

        if (isUnderage(request.dateOfBirth(), properties.registration().minimumAge(), clock)) {
            throw new CustomerUnderageException();
        }

        boolean countryAllowed = properties.registration().allowedCountries().stream()
                .map(value -> value.toUpperCase(Locale.ROOT))
                .anyMatch(country::equals);
        if (!countryAllowed) {
            throw new CountryNotAllowedException();
        }

        databaseOperations.acquirePermit();

        String password = generatePassword(random);
        String hash = encoder.encode(password);

        String iban = generateIban(
                properties.account().ibanCountryCode(),
                properties.account().ibanBankCode(),
                random
        );

        persist(request, username, country, hash, iban);

        return new RegisterResponse(username, password);
    }

    static boolean isUnderage(LocalDate dateOfBirth, int minimumAge, Clock clock) {
        return dateOfBirth.plusYears(minimumAge).isAfter(LocalDate.now(clock));
    }

    static String generatePassword(SecureRandom random) {
        byte[] entropy = new byte[16];
        random.nextBytes(entropy);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(entropy);
    }

    static String generateIban(String countryCode, String bankCode, SecureRandom random) {
        return new Iban.Builder(random)
                .countryCode(CountryCode.getByCode(countryCode.toUpperCase(Locale.ROOT)))
                .bankCode(bankCode.toUpperCase(Locale.ROOT))
                .buildRandom()
                .toString();
    }

    private void persist(RegisterRequest request, String username, String country, String hash, String iban) {
        AddressRequest address = request.address();
        Customer customer = new Customer(
                UUID.randomUUID(),
                request.fullName(),
                username,
                hash,
                request.dateOfBirth(),
                address.street(),
                address.houseNumber(),
                address.postalCode(),
                address.city(),
                country
        );
        customers.save(customer);

        Account account = new Account(
                UUID.randomUUID(),
                customer,
                iban,
                properties.account().type(),
                properties.account().initialBalance().setScale(2),
                properties.account().currency()
        );
        accounts.saveAndFlush(account);
    }

}
