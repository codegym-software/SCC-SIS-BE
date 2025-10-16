package com.example.sis.exceptions;

/**
 * Exception cho conflict nghiệp vụ (409)
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}