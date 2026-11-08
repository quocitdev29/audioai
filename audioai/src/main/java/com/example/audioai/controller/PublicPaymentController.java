package com.example.audioai.controller;

import com.example.audioai.dto.reponse.PaymentResultResponse;
import com.example.audioai.service.PaymentService;
import com.example.audioai.service.VnPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller cho các callback công khai từ VNPay (không yêu cầu authentication).
 */
@RestController
@RequestMapping("/api/public/payment/vnpay")
@RequiredArgsConstructor
public class PublicPaymentController {

    private static final Logger log = LoggerFactory.getLogger(PublicPaymentController.class);
    
    private final PaymentService paymentService;
    private final VnPayService vnPayService;

    /** URL frontend để redirect sau khi thanh toán (cấu hình qua app.frontend-url). */
    @org.springframework.beans.factory.annotation.Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    private static final String STATUS_SUCCESS = "SUCCESS";

    /**
     * GET /api/public/payment/vnpay/return
     * VNPay chuyển hướng trình duyệt về đây sau khi khách thanh toán xong.
     * Public (không cần token) vì VNPay gọi trực tiếp.
     * Redirect về frontend với params.
     */
    @GetMapping("/return")
    public ResponseEntity<Void> vnPayReturn(
            @RequestParam Map<String, String> allParams,
            HttpServletRequest httpRequest) {

        try {
            if (!vnPayService.verifySignature(allParams)) {
                // Redirect về frontend với status failed
                String redirectUrl = buildFrontendRedirectUrl("failed", null, allParams, 
                    "Chữ ký không hợp lệ");
                return ResponseEntity.status(HttpStatus.FOUND)
                        .header("Location", redirectUrl)
                        .build();
            }

            PaymentResultResponse data = paymentService.processVnPayCallback(allParams);
            
            // Redirect về frontend với params
            String status = STATUS_SUCCESS.equals(data.getStatus()) ? "success" : "failed";
            String redirectUrl = buildFrontendRedirectUrl(status, data, allParams, data.getMessage());
            
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", redirectUrl)
                    .build();
        } catch (Exception e) {
            log.error("Error processing VNPay return callback", e);
            String redirectUrl = buildFrontendRedirectUrl("failed", null, allParams, 
                "Lỗi xử lý thanh toán");
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", redirectUrl)
                    .build();
        }
    }

    /**
     * GET /api/public/payment/vnpay/ipn
     * VNPay gọi server-to-server để cập nhật kết quả thanh toán.
     * Response PHẢI đúng định dạng VNPAY yêu cầu: {"RspCode":"00","Message":"Confirm Success"}.
     */
    @GetMapping("/ipn")
    public ResponseEntity<Map<String, String>> vnPayIpn(@RequestParam Map<String, String> allParams) {
        if (!vnPayService.verifySignature(allParams)) {
            Map<String, String> invalid = new LinkedHashMap<>();
            invalid.put("RspCode", "97");
            invalid.put("Message", "Invalid Checksum");
            return ResponseEntity.ok(invalid);
        }
        return ResponseEntity.ok(paymentService.processIpn(allParams));
    }

    private String buildFrontendRedirectUrl(String status, PaymentResultResponse data, 
            Map<String, String> params, String message) {
        StringBuilder url = new StringBuilder("http://localhost:5173/payment/result");
        url.append("?status=").append(status);
        
        if (data != null) {
            if (data.getAmount() != null) {
                url.append("&amount=").append(data.getAmount());
            }
            if (data.getTransactionCode() != null) {
                url.append("&transactionCode=").append(data.getTransactionCode());
            }
        } else {
            // Fallback từ params
            String txnRef = params.get("vnp_TxnRef");
            String amount = params.get("vnp_Amount");
            if (txnRef != null) {
                url.append("&transactionCode=").append(txnRef);
            }
            if (amount != null) {
                // VNPay amount * 100, cần chia 100
                try {
                    long amountValue = Long.parseLong(amount) / 100;
                    url.append("&amount=").append(amountValue);
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        }
        
        if (message != null) {
            url.append("&message=").append(java.net.URLEncoder.encode(message, 
                java.nio.charset.StandardCharsets.UTF_8));
        }
        url.append("&timestamp=").append(java.time.LocalDateTime.now().toString());
        
        return url.toString();
    }
}
