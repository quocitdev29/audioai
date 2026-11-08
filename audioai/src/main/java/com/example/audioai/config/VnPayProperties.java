package com.example.audioai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cấu hình tích hợp cổng thanh toán VNPay (prefix "vnpay").
 */
@Data
@Component
@ConfigurationProperties(prefix = "vnpay")
public class VnPayProperties {

    /** Mã website/merchant do VNPay cấp (vnp_TmnCode). */
    private String tmnCode;

    /** Chuỗi bí mật dùng để ký HMAC-SHA512 (vnp_HashSecret). */
    private String hashSecret;

    /** URL cổng thanh toán VNPay. */
    private String payUrl;

    /** URL trình duyệt được VNPay chuyển hướng về sau khi thanh toán. */
    private String returnUrl;

    /** URL VNPay gọi server-to-server để thông báo kết quả (IPN). */
    private String ipnUrl;

    /** Phiên bản API. */
    private String version = "2.1.0";

    /** Mã API, giao dịch thanh toán là "pay". */
    private String command = "pay";

    /** Đơn vị tiền tệ. */
    private String currCode = "VND";

    /** Ngôn ngữ hiển thị: vn / en. */
    private String locale = "vn";

    /** Loại đơn hàng. */
    private String orderType = "other";
}
