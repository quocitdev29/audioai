package com.example.audioai.controller;

import com.example.audioai.dto.reponse.ApiResponse;
import com.example.audioai.dto.reponse.StoryDTO;
import com.example.audioai.dto.requests.CreateStoryRequest;
import com.example.audioai.dto.requests.UpdateStoryRequest;
import com.example.audioai.service.FileStorageService;
import com.example.audioai.service.StoryService;
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
 * Controller quản lý truyện cho admin.
 */
@RestController
@RequestMapping("/api/admin/stories")
@RequiredArgsConstructor
public class AdminStoryController extends BaseAdminController {

    private final StoryService storyService;
    private final FileStorageService fileStorageService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<StoryDTO>>> getAllStories(
            @AuthenticationPrincipal UserDetails userDetails) {
        checkAdminPermission(userDetails);
        List<StoryDTO> stories = storyService.getAllStories();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách truyện thành công", stories));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StoryDTO>> getStoryById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        checkAdminPermission(userDetails);
        StoryDTO story = storyService.getStoryById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin truyện thành công", story));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StoryDTO>> createStory(
            @Valid @RequestBody CreateStoryRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        checkAdminPermission(userDetails);
        StoryDTO story = storyService.createStory(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo truyện thành công", story));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StoryDTO>> updateStory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStoryRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        checkAdminPermission(userDetails);
        StoryDTO story = storyService.updateStory(id, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật truyện thành công", story));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStory(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        checkAdminPermission(userDetails);
        storyService.deleteStory(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Xóa truyện thành công", null));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<List<StoryDTO>>> getStoriesByCategory(
            @PathVariable Long categoryId,
            @AuthenticationPrincipal UserDetails userDetails) {
        checkAdminPermission(userDetails);
        List<StoryDTO> stories = storyService.getStoriesByCategory(categoryId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách truyện theo thể loại thành công", stories));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<StoryDTO>>> getStoriesByStatus(
            @PathVariable String status,
            @AuthenticationPrincipal UserDetails userDetails) {
        checkAdminPermission(userDetails);
        List<StoryDTO> stories = storyService.getStoriesByStatus(status);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách truyện theo trạng thái thành công", stories));
    }

    /**
     * Upload ảnh bìa truyện (chỉ admin).
     * Ảnh được lưu vào {@code Data/<TênTruyện>/<TênTruyện>.<ext>} và trả về
     * đường dẫn web {@code /Data/<TênTruyện>/<TênTruyện>.<ext>} để gán vào
     * {@code Story.coverUrl} khi tạo/cập nhật truyện.
     *
     * @param file  file ảnh (multipart/form-data, field name "file")
     * @param title tiêu đề truyện (multipart field "title") - dùng đặt tên thư mục/file
     */
    @PostMapping(value = "/upload-cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> uploadCover(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @AuthenticationPrincipal UserDetails userDetails) {
        checkAdminPermission(userDetails);
        String coverPath = fileStorageService.saveStoryCover(file, title);
        return ResponseEntity.ok(ApiResponse.success("Tải ảnh bìa thành công", coverPath));
    }
}
