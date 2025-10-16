package com.example.sis.exceptions;

/**
 * Exception khi cố gắng gán giảng viên đã có assignment active
 */
public class LecturerAlreadyAssignedException extends RuntimeException {
    public LecturerAlreadyAssignedException(String message) {
        super(message);
    }
}