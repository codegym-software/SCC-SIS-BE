package com.example.sis.exceptions;

public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}

class NoChangesException extends RuntimeException {
    public NoChangesException(String message) {
        super(message);
    }
}