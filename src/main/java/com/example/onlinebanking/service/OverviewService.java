package com.example.onlinebanking.service;

import com.example.onlinebanking.api.OverviewResponse;
import com.example.onlinebanking.persistence.Account;
import com.example.onlinebanking.persistence.AccountRepository;
import com.example.onlinebanking.service.exception.AccountNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OverviewService {
    private final AccountRepository accounts;

    public OverviewService(AccountRepository accounts) {
        this.accounts = accounts;
    }

    public OverviewResponse overview(UUID customerId) {
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
