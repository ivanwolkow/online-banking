package com.example.onlinebanking.persistence;

public class DatabaseBusyException extends RuntimeException {
    public DatabaseBusyException() {
        super("A database permit could not be obtained in time");
    }
}
