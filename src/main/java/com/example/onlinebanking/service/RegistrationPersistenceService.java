package com.example.onlinebanking.service;

import com.example.onlinebanking.config.AppProperties;
import com.example.onlinebanking.model.AddressRequest;
import com.example.onlinebanking.model.RegisterRequest;
import com.example.onlinebanking.persistence.Account;
import com.example.onlinebanking.persistence.AccountRepository;
import com.example.onlinebanking.persistence.Customer;
import com.example.onlinebanking.persistence.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RegistrationPersistenceService {
    private final CustomerRepository customers;
    private final AccountRepository accounts;
    private final AppProperties properties;

    public RegistrationPersistenceService(
            CustomerRepository customers,
            AccountRepository accounts,
            AppProperties properties
    ) {
        this.customers = customers;
        this.accounts = accounts;
        this.properties = properties;
    }

    @Transactional
    public void createCustomerAndAccount(
            RegisterRequest request,
            String username,
            String country,
            String passwordHash,
            String iban
    ) {
        AddressRequest address = request.address();
        Customer customer = new Customer(
                UUID.randomUUID(),
                request.fullName(),
                username,
                passwordHash,
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
