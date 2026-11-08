package com.example.audioai.controller;

import com.example.audioai.dto.reponse.ApiResponse;
import com.example.audioai.dto.reponse.PaymentUrlResponse;
import com.example.audioai.dto.requests.TopUpRequest;
import com.example.audioai.service.PaymentService;
import com.example.audioai.util.VnPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller cho các endpoint thanh toán của user (yêu cầu authentication).
 */
@RestController
@RequestMapping("/api/user/payment/vnpay")
@RequiredArgsConstructor
public class UserPaymentController {

    private final PaymentService paymentService;

    /**
     * POST /api/user/payment/vnpay/create-payment
     * Tạo URL thanh toán VNPay cho người dùng ĐANG ĐĂNG NHẬP (yêu cầu Bearer token).
     * Giao dịch PENDING được lưu gắn với chính user đang đăng nhập.
     */
    @PostMapping("/create-payment")
    public ResponseEntity<ApiResponse<PaymentUrlResponse>> createPayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TopUpRequest request,
            HttpServletRequest httpRequest) {

        PaymentUrlResponse data = paymentService.createTopUpPayment(
                userDetails.getUsername(), request, VnPayUtil.getIpAddress(httpRequest));

        return ResponseEntity.ok(ApiResponse.success("Tạo URL thanh toán thành công", data));
    }
}
