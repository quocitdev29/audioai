package com.example.audioai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cấu hình JWT được nạp từ application.properties (prefix "jwt").
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** Khóa bí mật (Base64) dùng để ký token HS256, bắt buộc >= 256 bits. */
    private String secret;

    /** Thời gian sống của token tính bằng milli giây (mặc định 1 ngày). */
    private long expiration = 86400000L;
}
