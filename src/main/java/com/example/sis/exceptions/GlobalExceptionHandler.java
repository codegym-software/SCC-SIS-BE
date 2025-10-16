package com.example.sis.exceptions;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.stream.Collectors;

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

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(ResourceNotFoundException ex, WebRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiError> handleValidation(ValidationException ex, WebRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(LecturerAlreadyAssignedException.class)
    public ResponseEntity<ApiError> handleLecturerAlreadyAssigned(LecturerAlreadyAssignedException ex, WebRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(ClassMaxActiveLecturersExceededException.class)
    public ResponseEntity<ApiError> handleClassMaxActiveLecturersExceeded(ClassMaxActiveLecturersExceededException ex, WebRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(AssignmentNotFoundException.class)
    public ResponseEntity<ApiError> handleAssignmentNotFound(AssignmentNotFoundException ex, WebRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex, WebRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    // Xử lý AuthenticationException và AccessDeniedException (403)
    @ExceptionHandler({AuthenticationException.class, AccessDeniedException.class})
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, WebRequest req) {
        return build(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện hành động này.", req);
    }

    // Xử lý UnexpectedRollbackException và TransactionSystemException (409)
    @ExceptionHandler({UnexpectedRollbackException.class, TransactionSystemException.class})
    public ResponseEntity<ApiError> handleTransaction(Exception ex, WebRequest req) {
        return build(HttpStatus.CONFLICT, "Không thể hoàn tất do vi phạm ràng buộc dữ liệu. Vui lòng thử lại.", req);
    }

    // Xử lý DataIntegrityViolationException và ConstraintViolationException (400)
    @ExceptionHandler({DataIntegrityViolationException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiError> handleDataIntegrity(Exception ex, WebRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ hoặc vi phạm ràng buộc.", req);
    }

    // Xử lý MethodArgumentNotValidException với thông báo tiếng Việt
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiError> handleValidation(Exception ex, WebRequest req) {
        String message = "Dữ liệu không hợp lệ.";

        if (ex instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException validEx = (MethodArgumentNotValidException) ex;
            List<String> errors = validEx.getBindingResult()
                    .getFieldErrors()
                    .stream()
                    .map(this::getFieldErrorMessage)
                    .collect(Collectors.toList());

            if (!errors.isEmpty()) {
                message = String.join(". ", errors) + ".";
            }
        }

        return build(HttpStatus.BAD_REQUEST, message, req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleOther(Exception ex, WebRequest req) {
        // Không trả về thông tin kỹ thuật cho client
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.", req);
    }

    private String getFieldErrorMessage(FieldError fieldError) {
        String field = fieldError.getField();
        String message = fieldError.getDefaultMessage();

        // Chuyển đổi một số thông báo lỗi phổ biến sang tiếng Việt
        switch (message) {
            case "must not be blank":
                return "Trường " + field + " không được để trống";
            case "must not be null":
                return "Trường " + field + " là bắt buộc";
            case "size must be between":
                return "Trường " + field + " phải có độ dài hợp lệ";
            case "must be a valid email":
                return "Trường " + field + " phải là email hợp lệ";
            default:
                return "Trường " + field + " không hợp lệ";
        }
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
