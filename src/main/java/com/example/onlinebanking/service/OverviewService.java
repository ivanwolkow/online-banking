package com.example.onlinebanking.service;

import com.example.onlinebanking.api.OverviewResponse;
import com.example.onlinebanking.exception.AccountNotFoundException;
import com.example.onlinebanking.persistence.Account;
import com.example.onlinebanking.persistence.AccountRepository;
import com.example.onlinebanking.persistence.DatabaseOperationGate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OverviewService {
    private final AccountRepository accounts;
    private final DatabaseOperationGate databaseOperations;

    public OverviewService(AccountRepository accounts, DatabaseOperationGate databaseOperations) {
        this.accounts = accounts;
        this.databaseOperations = databaseOperations;
    }

    public OverviewResponse overview(UUID customerId) {
        databaseOperations.acquirePermit();

        Account account = accounts.findByCustomerId(customerId)
                .orElseThrow(AccountNotFoundException::new);

        return new OverviewResponse(
                account.getIban(),
                account.getAccountType(),
                account.getBalance(),
                account.getCurrency()
        );
    }
}
