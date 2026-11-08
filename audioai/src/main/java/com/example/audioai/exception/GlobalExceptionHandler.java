package com.example.audioai.exception;

import com.example.audioai.dto.reponse.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bắt toàn bộ ngoại lệ và trả về {@link ApiResponse} thống nhất với thông báo tiếng Việt.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** Lỗi nghiệp vụ do ứng dụng chủ động ném ra. */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex) {
        return buildResponse(ex.getStatus(), ex.getMessage(), null);
    }

    /** Lỗi validate dữ liệu đầu vào (@Valid). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return buildResponse(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ", errors);
    }

    /** Body request rỗng hoặc JSON sai định dạng. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadableException(HttpMessageNotReadableException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST,
                "Nội dung yêu cầu không hợp lệ hoặc sai định dạng JSON", null);
    }

    /** Không tìm thấy tài nguyên tĩnh (ảnh/file trong /Data, /assets...). */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Không tìm thấy tài nguyên", null);
    }

    /** Sai tên đăng nhập hoặc mật khẩu. */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(BadCredentialsException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Tên đăng nhập hoặc mật khẩu không đúng", null);
    }

    /** Tài khoản bị khóa / vô hiệu hóa. */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabledException(DisabledException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, "Tài khoản đã bị vô hiệu hóa", null);
    }

    /** Không tìm thấy người dùng (trả về chung để tránh lộ thông tin). */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Tên đăng nhập hoặc mật khẩu không đúng", null);
    }

    /** Không đủ quyền truy cập. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập tài nguyên này", null);
    }

    /** Các lỗi xác thực còn lại. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Xác thực không thành công, vui lòng đăng nhập lại", null);
    }

    /** Lỗi không mong đợi. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Lỗi hệ thống không mong đợi: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Đã xảy ra lỗi hệ thống, vui lòng thử lại sau", null);
    }

    private ResponseEntity<ApiResponse<Void>> buildResponse(HttpStatus status,
                                                            String message,
                                                            Map<String, String> errors) {
        return ResponseEntity.status(status).body(ApiResponse.error(status, message, errors));
    }
}

