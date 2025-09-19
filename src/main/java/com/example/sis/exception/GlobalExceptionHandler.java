package com.example.sis.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex, WebRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex, WebRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiError> handleValidation(Exception ex, WebRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Validation failed: " + ex.getMessage(), req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, WebRequest req) {
        return build(HttpStatus.CONFLICT, "Vi phạm ràng buộc dữ liệu (ví dụ email đã tồn tại).", req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleOther(Exception ex, WebRequest req) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), req);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, WebRequest req) {
        ApiError err = new ApiError();
        err.setStatus(status.value());
        err.setError(status.getReasonPhrase());
        err.setMessage(message);
        err.setPath(req.getDescription(false));
        return ResponseEntity.status(status).body(err);
    }
}
