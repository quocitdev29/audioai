package com.example.audioai.dto.reponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Wrapper chuẩn cho toàn bộ response của API.
 *
 * <pre>
 * Thành công: { "success": true,  "code": 200, "message": "...", "data": {...},   "timestamp": "..." }
 * Thất bại : { "success": false, "code": 400, "message": "...", "errors": {...}, "timestamp": "..." }
 * </pre>
 *
 * @param <T> kiểu dữ liệu của payload trả về
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    /** {@code true} nếu request được xử lý thành công. */
    private boolean success;

    /** Mã trạng thái HTTP. */
    private int code;

    /** Thông báo mô tả kết quả (tiếng Việt). */
    private String message;

    /** Dữ liệu trả về - chỉ xuất hiện khi thành công. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;

    /** Chi tiết lỗi theo từng trường - chỉ xuất hiện khi validate thất bại. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, String> errors;

    /** Thời điểm tạo phản hồi. */
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(String message, T data) {
        return success(HttpStatus.OK, message, data);
    }

    public static <T> ApiResponse<T> success(HttpStatus status, String message, T data) {
        return success(status.value(), message, data);
    }

    public static <T> ApiResponse<T> success(int code, String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code(code)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> error(HttpStatus status, String message) {
        return error(status, message, null);
    }

    public static <T> ApiResponse<T> error(HttpStatus status, String message, Map<String, String> errors) {
        return error(status.value(), message, errors);
    }

    public static <T> ApiResponse<T> error(int code, String message, Map<String, String> errors) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(code)
                .message(message)
                .errors(errors)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
