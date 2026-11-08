package com.example.audioai.util;

import jakarta.servlet.http.HttpServletRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Tiện ích dùng chung cho tích hợp VNPay (ký HMAC-SHA512, dựng chuỗi hash, lấy IP).
 */
public final class VnPayUtil {

    /** VNPay yêu cầu thời gian theo GMT+7. */
    public static final ZoneId VNPAY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private static final DateTimeFormatter VNPAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private VnPayUtil() {
    }

    /** Tạo chữ ký HMAC-SHA512 (hex thường) theo key bí mật. */
    public static String hmacSHA512(String key, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            hmac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(String.format("%02x", b & 0xff));
            }
            return hex.toString();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Không thể tạo chữ ký HMAC-SHA512", ex);
        }
    }

    /**
     * Dựng chuỗi dữ liệu để ký: sắp xếp key theo alphabet, nối dạng {@code key=value}
     * (value đã URL-encode UTF-8).
     */
    public static String buildHashData(Map<String, String> params) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String fieldName : fieldNames) {
            String fieldValue = params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                if (!first) {
                    sb.append('&');
                }
                sb.append(fieldName)
                        .append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
                first = false;
            }
        }
        return sb.toString();
    }

    /** Thời gian hiện tại theo định dạng VNPay yyyyMMddHHmmss (GMT+7). */
    public static String now() {
        return ZonedDateTime.now(VNPAY_ZONE).format(VNPAY_DATE_FORMAT);
    }

    /** Lấy IP của client, ưu tiên header X-Forwarded-For khi chạy sau proxy. */
    public static String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return (ip == null || ip.isBlank()) ? "127.0.0.1" : ip;
    }
}
