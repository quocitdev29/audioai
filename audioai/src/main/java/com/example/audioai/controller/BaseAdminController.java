package com.example.audioai.controller;

import com.example.audioai.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Base controller cho tất cả admin endpoints.
 * Cung cấp phương thức checkAdminPermission() dùng chung.
 */
public abstract class BaseAdminController {

    /**
     * Kiểm tra quyền admin của user hiện tại.
     * 
     * @param userDetails thông tin user từ @AuthenticationPrincipal
     * @throws ApiException với HTTP 403 nếu user không phải admin
     */
    protected void checkAdminPermission(UserDetails userDetails) {
        if (!"admin".equals(userDetails.getUsername())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập");
        }
    }
}
