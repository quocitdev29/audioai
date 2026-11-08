package com.example.audioai.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OwnershipCheckResponse {
    
    private Boolean owned;
    private LocalDateTime purchasedAt;
    private String orderNumber;
}
