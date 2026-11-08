package com.example.audioai.dto.reponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Trả về URL thanh toán VNPay cho client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentUrlResponse {
    private String transactionCode;
    private BigDecimal amount;
    private String paymentUrl;
}
