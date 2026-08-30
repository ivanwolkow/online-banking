package com.example.onlinebanking.service;

import com.example.onlinebanking.config.AppProperties;
import com.example.onlinebanking.exception.CountryNotAllowedException;
import com.example.onlinebanking.exception.CustomerUnderageException;
import com.example.onlinebanking.model.RegisterRequest;
import com.example.onlinebanking.model.RegisterResponse;
import com.example.onlinebanking.persistence.DatabaseOperationGate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Locale;

@Service
public class RegistrationService {
    private final AppProperties properties;
    private final Clock clock;
    private final PasswordEncoder encoder;
    private final SecureRandom random;
    private final IbanProvider ibanProvider;
    private final DatabaseOperationGate databaseOperations;
    private final RegistrationPersistenceService persistence;

    public RegistrationService(
            AppProperties properties,
            Clock clock,
            PasswordEncoder encoder,
            SecureRandom random,
            IbanProvider ibanProvider,
            DatabaseOperationGate databaseOperations,
            RegistrationPersistenceService persistence
    ) {
        this.properties = properties;
        this.clock = clock;
        this.encoder = encoder;
        this.random = random;
        this.ibanProvider = ibanProvider;
        this.databaseOperations = databaseOperations;
        this.persistence = persistence;
    }

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

        String password = generatePassword(random);
        String hash = encoder.encode(password);

        String iban = ibanProvider.provide();

        databaseOperations.acquirePermit();
        persistence.createCustomerAndAccount(request, username, country, hash, iban);

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

}
