package com.example.audioai.dto.reponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO cho lịch sử thanh toán (admin xem)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentHistoryResponse {
    private Long id;
    private String username;
    private String hovaten;
    private String type;
    private BigDecimal amount;
    private String status; // SUCCESS, FAILED, PENDING
    private LocalDateTime createdAt;
}
