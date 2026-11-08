package com.example.audioai.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PurchaseStoryResponse {
    
    private Long orderId;
    private String orderNumber;
    private Long storyId;
    private String storyTitle;
    private BigDecimal amount;
    private String status;
    private LocalDateTime purchasedAt;
    private BigDecimal newBalance;
}
