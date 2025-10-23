package com.example.store.exception;

public class SqlPerformanceException extends RuntimeException {
    public SqlPerformanceException(String message) {
        super(message);
    }
}