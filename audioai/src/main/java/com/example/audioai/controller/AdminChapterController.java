package com.example.audioai.controller;

import com.example.audioai.dto.reponse.ApiResponse;
import com.example.audioai.dto.reponse.ChapterDTO;
import com.example.audioai.dto.requests.CreateChapterRequest;
import com.example.audioai.dto.requests.UpdateChapterRequest;
import com.example.audioai.service.ChapterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Controller quản lý chương truyện cho admin.
 */
@RestController
@RequestMapping("/api/admin/chapters")
@RequiredArgsConstructor
public class AdminChapterController extends BaseAdminController {

    private final ChapterService chapterService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ChapterDTO>>> getAllChapters(
            @AuthenticationPrincipal UserDetails userDetails) {
        checkAdminPermission(userDetails);
        List<ChapterDTO> chapters = chapterService.getAllChapters();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách chương thành công", chapters));
    }

    @GetMapping("/story/{storyId}")
    public ResponseEntity<ApiResponse<List<ChapterDTO>>> getChaptersByStory(
            @PathVariable Long storyId,
            @AuthenticationPrincipal UserDetails userDetails) {
        checkAdminPermission(userDetails);
        List<ChapterDTO> chapters = chapterService.getChaptersByStory(storyId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách chương theo truyện thành công", chapters));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ChapterDTO>> getChapterById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        checkAdminPermission(userDetails);
        ChapterDTO chapter = chapterService.getChapterById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin chương thành công", chapter));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ChapterDTO>> createChapter(
            @Valid @RequestBody CreateChapterRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        checkAdminPermission(userDetails);
        ChapterDTO chapter = chapterService.createChapter(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm chương thành công", chapter));
    }

    /**
     * Upload file audio của chương (multipart/form-data).
     * File lưu vào {@code Data/<TênTruyện>/} và trả về đường dẫn {@code /Data/...}
     * để gán vào {@code audioUrl} khi thêm/cập nhật chương.
     *
     * @param file    file audio (field name "file")
     * @param storyId ID truyện (field name "storyId")
     */
    @PostMapping(value = "/upload-audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> uploadAudio(
            @RequestParam("file") MultipartFile file,
            @RequestParam("storyId") Long storyId,
            @AuthenticationPrincipal UserDetails userDetails) {
        checkAdminPermission(userDetails);
        String audioUrl = chapterService.uploadChapterAudio(file, storyId);
        return ResponseEntity.ok(ApiResponse.success("Tải file audio thành công", audioUrl));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ChapterDTO>> updateChapter(
            @PathVariable Long id,
            @Valid @RequestBody UpdateChapterRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        checkAdminPermission(userDetails);
        ChapterDTO chapter = chapterService.updateChapter(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật chương thành công", chapter));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteChapter(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        checkAdminPermission(userDetails);
        chapterService.deleteChapter(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa chương thành công", null));
    }
}
