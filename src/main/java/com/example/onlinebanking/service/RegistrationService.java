package com.example.onlinebanking.service;

import com.example.onlinebanking.api.AddressRequest;
import com.example.onlinebanking.api.RegisterRequest;
import com.example.onlinebanking.api.RegisterResponse;
import com.example.onlinebanking.config.AppProperties;
import com.example.onlinebanking.exception.AccountNumberGenerationFailedException;
import com.example.onlinebanking.exception.CountryNotAllowedException;
import com.example.onlinebanking.exception.CustomerUnderageException;
import com.example.onlinebanking.exception.UsernameAlreadyExistsException;
import com.example.onlinebanking.persistence.Account;
import com.example.onlinebanking.persistence.AccountRepository;
import com.example.onlinebanking.persistence.Customer;
import com.example.onlinebanking.persistence.CustomerRepository;
import com.example.onlinebanking.persistence.DatabaseOperationGate;
import org.hibernate.exception.ConstraintViolationException;
import org.iban4j.CountryCode;
import org.iban4j.Iban;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

@Service
public class RegistrationService {
    private static final int IBAN_ATTEMPTS = 5;
    private final CustomerRepository customers;
    private final AccountRepository accounts;
    private final TransactionTemplate transactions;
    private final AppProperties properties;
    private final Clock clock;
    private final PasswordEncoder encoder;
    private final SecureRandom random;
    private final DatabaseOperationGate databaseOperations;

    public RegistrationService(
            CustomerRepository customers,
            AccountRepository accounts,
            TransactionTemplate transactions,
            AppProperties properties,
            Clock clock,
            PasswordEncoder encoder,
            SecureRandom random,
            DatabaseOperationGate databaseOperations
    ) {
        this.customers = customers;
        this.accounts = accounts;
        this.transactions = transactions;
        this.properties = properties;
        this.clock = clock;
        this.encoder = encoder;
        this.random = random;
        this.databaseOperations = databaseOperations;
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

        databaseOperations.acquirePermit();

        String password = generatePassword(random);
        String hash = encoder.encode(password);

        for (int attempt = 0; attempt < IBAN_ATTEMPTS; attempt++) {
            try {
                String iban = generateIban(
                        properties.account().ibanCountryCode(),
                        properties.account().ibanBankCode(),
                        random
                );
                transactions.executeWithoutResult(status -> persist(request, username, country, hash, iban));

                return new RegisterResponse(username, password);
            } catch (DataIntegrityViolationException exception) {
                if (isUsernameConflict(exception)) {
                    throw new UsernameAlreadyExistsException();
                }
                if (!isIbanConflict(exception)) {
                    throw exception;
                }
            }
        }

        throw new AccountNumberGenerationFailedException();
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

    private static boolean isUsernameConflict(Exception exception) {
        return hasConstraint(exception, "uk_customers_username");
    }

    private static boolean isIbanConflict(Exception exception) {
        return hasConstraint(exception, "uk_accounts_iban");
    }

    private static boolean hasConstraint(Throwable exception, String expectedName) {
        for (Throwable current = exception; current != null; current = current.getCause()) {
            if (current instanceof ConstraintViolationException violation
                    && expectedName.equalsIgnoreCase(violation.getConstraintName())) {
                return true;
            }
        }

        return false;
    }
}
