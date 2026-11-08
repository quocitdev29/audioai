package com.example.audioai.service;

import com.example.audioai.config.VnPayProperties;
import com.example.audioai.util.VnPayUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * Thực thi giao thức VNPay: dựng URL thanh toán và kiểm tra chữ ký HMAC-SHA512.
 */
@Service
@RequiredArgsConstructor
public class VnPayService {

    private final VnPayProperties vnPayProperties;

    /**
     * Dựng URL thanh toán VNPay (phương thức GET).
     *
     * @param transactionCode mã giao dịch phía merchant (vnp_TxnRef)
     * @param amount          số tiền VNĐ (sẽ nhân 100 theo yêu cầu VNPay)
     * @param orderInfo       mô tả đơn hàng
     * @param ipAddress       IP của khách hàng
     */
    public String createPaymentUrl(String transactionCode, BigDecimal amount, String orderInfo, String ipAddress) {
        long vnpAmount = amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", vnPayProperties.getVersion());
        params.put("vnp_Command", vnPayProperties.getCommand());
        params.put("vnp_TmnCode", vnPayProperties.getTmnCode());
        params.put("vnp_Amount", String.valueOf(vnpAmount));
        params.put("vnp_CurrCode", vnPayProperties.getCurrCode());
        params.put("vnp_TxnRef", transactionCode);
        params.put("vnp_OrderInfo", orderInfo);
        params.put("vnp_OrderType", vnPayProperties.getOrderType());
        params.put("vnp_Locale", vnPayProperties.getLocale());
        params.put("vnp_ReturnUrl", vnPayProperties.getReturnUrl());
        params.put("vnp_IpAddr", ipAddress);
        params.put("vnp_CreateDate", VnPayUtil.now());

        String hashData = VnPayUtil.buildHashData(params);
        String secureHash = VnPayUtil.hmacSHA512(vnPayProperties.getHashSecret(), hashData);

        return vnPayProperties.getPayUrl() + "?" + hashData + "&vnp_SecureHash=" + secureHash;
    }

    /**
     * Kiểm tra tính toàn vẹn dữ liệu VNPay gửi về.
     */
    public boolean verifySignature(Map<String, String> params) {
        String secureHash = params.get("vnp_SecureHash");
        if (secureHash == null || secureHash.isBlank()) {
            return false;
        }

        Map<String, String> fields = new HashMap<>(params);
        fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");

        String hashData = VnPayUtil.buildHashData(fields);
        String expected = VnPayUtil.hmacSHA512(vnPayProperties.getHashSecret(), hashData);
        return expected.equalsIgnoreCase(secureHash);
    }

    /**
     * Diễn giải mã phản hồi VNPay sang tiếng Việt.
     */
    public String getResponseMessage(String responseCode) {
        if (responseCode == null || responseCode.isBlank()) {
            return "Không xác định được kết quả giao dịch";
        }
        return switch (responseCode) {
            case "00" -> "Giao dịch thành công";
            case "07" -> "Trừ tiền thành công nhưng giao dịch bị nghi ngờ gian lận";
            case "09" -> "Thẻ/Tài khoản chưa đăng ký dịch vụ InternetBanking";
            case "10" -> "Xác thực thông tin thẻ/tài khoản sai quá 3 lần";
            case "11" -> "Đã hết hạn chờ thanh toán";
            case "12" -> "Thẻ/Tài khoản của khách hàng bị khóa";
            case "13" -> "Nhập sai mật khẩu xác thực giao dịch (OTP)";
            case "24" -> "Khách hàng hủy giao dịch";
            case "51" -> "Tài khoản không đủ số dư để thực hiện giao dịch";
            case "65" -> "Tài khoản đã vượt quá hạn mức giao dịch trong ngày";
            case "75" -> "Ngân hàng thanh toán đang bảo trì";
            case "79" -> "Nhập sai mật khẩu thanh toán quá số lần quy định";
            case "99" -> "Lỗi khác, vui lòng liên hệ VNPAY";
            default -> "Giao dịch không thành công (mã phản hồi: " + responseCode + ")";
        };
    }
}
