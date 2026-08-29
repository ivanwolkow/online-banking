package com.example.onlinebanking.service.exception;

public abstract class DomainException extends RuntimeException {
    private final String code;
    private final String title;

    protected DomainException(String code, String title, String detail) {
        super(detail);
        this.code = code;
        this.title = title;
    }

    public String code() {
        return code;
    }

    public String title() {
        return title;
    }
}
