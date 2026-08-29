package com.example.onlinebanking.persistence;

public final class SchemaConstraints {
    public static final String CUSTOMER_USERNAME_UNIQUE = "uk_customers_username";
    public static final String ACCOUNT_CUSTOMER_UNIQUE = "uk_accounts_customer";
    public static final String ACCOUNT_IBAN_UNIQUE = "uk_accounts_iban";

    private SchemaConstraints() {
    }
}
