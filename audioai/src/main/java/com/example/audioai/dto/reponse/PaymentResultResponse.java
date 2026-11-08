package com.example.audioai.dto.reponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Kết quả xử lý giao dịch nạp tiền VNPay.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResultResponse {
    private String transactionCode;
    private String vnpTransactionNo;
    private String bankCode;
    private String responseCode;
    private String transactionStatus;
    private String status;
    private BigDecimal amount;
    private BigDecimal balance;
    private String message;
}
