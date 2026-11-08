package com.example.audioai.dto.reponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {
    private Long id;
    private String name;
    /** Slug sinh từ name - dùng để dựng URL /category/{slug} ở frontend. */
    private String slug;
}
