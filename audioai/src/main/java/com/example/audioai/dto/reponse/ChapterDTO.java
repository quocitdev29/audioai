package com.example.audioai.dto.reponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterDTO {
    private Long id;
    private Long storyId;
    private String storyTitle;
    private Integer chapterNumber;
    private String title;
    private String content;
    private String audioUrl;
    private Boolean isFree;
    private LocalDateTime createdAt;
}