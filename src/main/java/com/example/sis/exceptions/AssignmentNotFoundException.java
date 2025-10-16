package com.example.sis.exceptions;

/**
 * Exception khi không tìm thấy assignment để thực hiện thao tác
 */
public class AssignmentNotFoundException extends RuntimeException {
    public AssignmentNotFoundException(String message) {
        super(message);
    }
}