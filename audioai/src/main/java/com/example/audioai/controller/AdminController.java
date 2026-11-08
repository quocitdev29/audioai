package com.example.audioai.controller;

import com.example.audioai.dto.reponse.AdminStatsResponse;
import com.example.audioai.dto.reponse.ApiResponse;
import com.example.audioai.dto.reponse.PaymentHistoryResponse;
import com.example.audioai.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller cho admin
 * Chỉ ADMIN mới được truy cập các endpoint này
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController extends BaseAdminController {

    private final AdminService adminService;

    /**
     * GET /api/admin/statistics - Lấy thống kê tổng quan
     * Chỉ ADMIN
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getStatistics(
            @AuthenticationPrincipal UserDetails userDetails) {
        checkAdminPermission(userDetails);
        AdminStatsResponse stats = adminService.getStatistics();
        return ResponseEntity.ok(ApiResponse.success("Lấy thống kê thành công", stats));
    }

    /**
     * GET /api/admin/payments - Lấy danh sách lịch sử thanh toán
     * Chỉ ADMIN
     */
    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<List<PaymentHistoryResponse>>> getPaymentHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        checkAdminPermission(userDetails);
        List<PaymentHistoryResponse> payments = adminService.getPaymentHistory(page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử thanh toán thành công", payments));
    }
}
