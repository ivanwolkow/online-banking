package com.example.onlinebanking.persistence;

public class DatabaseBusyException extends RuntimeException {
    public DatabaseBusyException() {
        super("Database operation capacity is currently unavailable");
    }
}
