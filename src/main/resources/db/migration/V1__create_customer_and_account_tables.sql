CREATE TABLE customers (
    id UUID PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    street VARCHAR(100) NOT NULL,
    house_number VARCHAR(20) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    city VARCHAR(100) NOT NULL,
    country_code VARCHAR(2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_customers_username UNIQUE (username)
);

CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    iban VARCHAR(18) NOT NULL,
    account_type VARCHAR(20) NOT NULL,
    balance NUMERIC(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_accounts_customer UNIQUE (customer_id),
    CONSTRAINT uk_accounts_iban UNIQUE (iban),
    CONSTRAINT fk_accounts_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
);

CREATE INDEX idx_customers_username ON customers(username);
