package com.example.onlinebanking.service;

import com.example.onlinebanking.api.AddressRequest;
import com.example.onlinebanking.api.RegisterRequest;
import com.example.onlinebanking.api.RegisterResponse;
import com.example.onlinebanking.config.AppProperties;
import com.example.onlinebanking.domain.AgeEligibility;
import com.example.onlinebanking.domain.IbanGenerator;
import com.example.onlinebanking.domain.PasswordGenerator;
import com.example.onlinebanking.domain.UsernameNormalizer;
import com.example.onlinebanking.persistence.Account;
import com.example.onlinebanking.persistence.AccountRepository;
import com.example.onlinebanking.persistence.Customer;
import com.example.onlinebanking.persistence.CustomerRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.Locale;
import java.util.UUID;

@Service
public class RegistrationService {
    private static final int IBAN_ATTEMPTS = 5;
    private final CustomerRepository customers; private final AccountRepository accounts; private final TransactionTemplate transactions;
    private final AppProperties properties; private final Clock clock; private final PasswordGenerator passwords;
    private final PasswordEncoder encoder; private final IbanGenerator ibans;

    public RegistrationService(CustomerRepository customers, AccountRepository accounts, TransactionTemplate transactions,
                               AppProperties properties, Clock clock, PasswordGenerator passwords, PasswordEncoder encoder, IbanGenerator ibans) {
        this.customers = customers; this.accounts = accounts; this.transactions = transactions; this.properties = properties;
        this.clock = clock; this.passwords = passwords; this.encoder = encoder; this.ibans = ibans;
    }

    public RegisterResponse register(RegisterRequest request) {
        String username = UsernameNormalizer.normalize(request.username());
        String country = request.address().countryCode().trim().toUpperCase(Locale.ROOT);
        if (!AgeEligibility.isEligible(request.dateOfBirth(), properties.registration().minimumAge(), clock))
            throw new DomainException("CUSTOMER_UNDERAGE", "Customer has not reached the minimum age.");
        if (!properties.registration().allowedCountries().stream().map(value -> value.toUpperCase(Locale.ROOT)).anyMatch(country::equals))
            throw new DomainException("COUNTRY_NOT_ALLOWED", "The address country is not allowed.");
        String password = passwords.generate();
        String hash = encoder.encode(password);
        for (int attempt = 0; attempt < IBAN_ATTEMPTS; attempt++) {
            try {
                String iban = ibans.generate();
                transactions.executeWithoutResult(status -> persist(request, username, country, hash, iban));
                return new RegisterResponse(username, password);
            } catch (DataIntegrityViolationException exception) {
                if (isUsernameConflict(exception)) throw new DomainException("USERNAME_ALREADY_EXISTS", "The username is already in use.");
                if (!isIbanConflict(exception)) throw exception;
            }
        }
        throw new DomainException("ACCOUNT_NUMBER_GENERATION_FAILED", "Could not create a unique account number.");
    }

    private void persist(RegisterRequest request, String username, String country, String hash, String iban) {
        AddressRequest address = request.address();
        Customer customer = new Customer(UUID.randomUUID(), request.fullName().trim(), username, hash, request.dateOfBirth(),
                address.street().trim(), address.houseNumber().trim(), address.postalCode().trim(), address.city().trim(), country);
        customers.save(customer);
        accounts.saveAndFlush(new Account(UUID.randomUUID(), customer, iban, properties.account().type(),
                properties.account().initialBalance().setScale(2), properties.account().currency()));
    }

    private static boolean isUsernameConflict(Exception exception) { return message(exception).contains("uk_customers_username"); }
    private static boolean isIbanConflict(Exception exception) { return message(exception).contains("uk_accounts_iban"); }
    private static String message(Throwable exception) {
        StringBuilder result = new StringBuilder();
        for (Throwable current = exception; current != null; current = current.getCause()) result.append(current.getMessage()).append(' ');
        return result.toString().toLowerCase(Locale.ROOT);
    }
}
