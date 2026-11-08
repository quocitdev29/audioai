package com.example.audioai.dto.requests;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request cập nhật chương - chỉ cập nhật các trường khác null.
 * Không cho đổi truyện (storyId) sau khi tạo.
 */
@Data
public class UpdateChapterRequest {

    @Positive(message = "Số chương phải lớn hơn 0")
    private Integer chapterNumber;

    @Size(max = 255, message = "Tiêu đề chương không được vượt quá 255 ký tự")
    private String title;

    private String content;

    @Size(max = 500, message = "URL audio không được vượt quá 500 ký tự")
    private String audioUrl;

    private Boolean isFree;
}