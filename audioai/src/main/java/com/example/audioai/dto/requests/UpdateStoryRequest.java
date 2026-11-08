package com.example.audioai.dto.requests;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class UpdateStoryRequest {
    
    @Size(max = 255, message = "Tiêu đề không được vượt quá 255 ký tự")
    private String title;

    @Size(max = 65535, message = "Mô tả quá dài")
    private String description;

    @Size(max = 500, message = "URL ảnh bìa không được vượt quá 500 ký tự")
    private String coverUrl;

    private Long categoryId;

    @DecimalMin(value = "0", message = "Giá phải lớn hơn hoặc bằng 0")
    private BigDecimal price;

    @Size(max = 30, message = "Trạng thái không được vượt quá 30 ký tự")
    private String status;
}
