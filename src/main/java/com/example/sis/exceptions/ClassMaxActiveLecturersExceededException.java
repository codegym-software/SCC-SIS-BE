package com.example.sis.exceptions;

/**
 * Exception khi vượt quá số lượng tối đa giảng viên active cho lớp (3 giảng viên)
 */
public class ClassMaxActiveLecturersExceededException extends RuntimeException {
    public ClassMaxActiveLecturersExceededException(String message) {
        super(message);
    }
}