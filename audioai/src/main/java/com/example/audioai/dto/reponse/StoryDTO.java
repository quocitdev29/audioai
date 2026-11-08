package com.example.audioai.dto.reponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryDTO {
    private Long id;
    private String title;
    /** Slug sinh từ title - dùng để dựng URL /story/{slug} ở frontend. */
    private String slug;
    private String description;
    private String coverUrl;
    private BigDecimal price;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private CategoryDTO category;
    private CreatedByDTO createdBy;
    /** Số chương hiện có của truyện (dùng cho hero / thẻ truyện ở trang chủ). */
    private Long chapterCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatedByDTO {
        private Long id;
        private String username;
        private String hovaten;
    }
}
