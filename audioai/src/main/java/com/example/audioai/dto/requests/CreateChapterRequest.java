package com.example.audioai.dto.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateChapterRequest {

    @NotNull(message = "Truyện không được để trống")
    private Long storyId;

    @NotNull(message = "Số chương không được để trống")
    @Positive(message = "Số chương phải lớn hơn 0")
    private Integer chapterNumber;

    @NotBlank(message = "Tiêu đề chương không được để trống")
    @Size(max = 255, message = "Tiêu đề chương không được vượt quá 255 ký tự")
    private String title;

    private String content;

    @Size(max = 500, message = "URL audio không được vượt quá 500 ký tự")
    private String audioUrl;

    /** Chương miễn phí hay không (mặc định false nếu không truyền). */
    private Boolean isFree;
}