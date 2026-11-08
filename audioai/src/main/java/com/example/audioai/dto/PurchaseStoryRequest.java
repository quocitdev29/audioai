package com.example.audioai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PurchaseStoryRequest {
    
    @NotNull(message = "Story ID is required")
    private Long storyId;
}
