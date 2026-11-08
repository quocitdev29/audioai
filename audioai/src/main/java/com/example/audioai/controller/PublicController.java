package com.example.audioai.controller;

import com.example.audioai.dto.reponse.ApiResponse;
import com.example.audioai.dto.reponse.CategoryDTO;
import com.example.audioai.dto.reponse.ChapterDTO;
import com.example.audioai.dto.reponse.StoryDTO;
import com.example.audioai.entity.Category;
import com.example.audioai.service.CategoryService;
import com.example.audioai.service.ChapterService;
import com.example.audioai.service.StoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller cho các endpoint công khai (không yêu cầu authentication).
 * Bao gồm: stories, categories, chapters public.
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final StoryService storyService;
    private final CategoryService categoryService;
    private final ChapterService chapterService;

    /**
     * GET /api/public/stories
     * Lấy danh sách truyện cho trang chủ, sắp xếp mới nhất trước.
     *
     * @param status (tuỳ chọn) chỉ lấy truyện theo trạng thái, ví dụ {@code PUBLISHED}
     */
    @GetMapping("/stories")
    public ResponseEntity<ApiResponse<List<StoryDTO>>> getStories(
            @RequestParam(name = "status", required = false) String status) {
        List<StoryDTO> stories = storyService.getPublicStories(status);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách truyện thành công", stories));
    }

    /**
     * GET /api/public/stories/latest
     * Lấy truyện mới nhất để hiển thị ở section hero của trang chủ.
     *
     * @param status (tuỳ chọn) chỉ lấy truyện theo trạng thái, ví dụ {@code PUBLISHED}
     * @return truyện mới nhất, {@code data = null} nếu chưa có truyện nào
     */
    @GetMapping("/stories/latest")
    public ResponseEntity<ApiResponse<StoryDTO>> getLatestStory(
            @RequestParam(name = "status", required = false) String status) {
        StoryDTO story = storyService.getLatestStory(status);
        return ResponseEntity.ok(ApiResponse.success("Lấy truyện mới nhất thành công", story));
    }

    /**
     * GET /api/public/stories/category/{slug}
     * Lấy danh sách truyện theo thể loại (category) qua slug SEO của thể loại.
     *
     * @param slug slug của thể loại, ví dụ {@code tien-hiep}
     * @return danh sách truyện thuộc thể loại đó, sắp xếp mới nhất trước
     */
    @GetMapping("/stories/category/{slug}")
    public ResponseEntity<ApiResponse<List<StoryDTO>>> getStoriesByCategory(
            @PathVariable String slug) {
        Category category = categoryService.getCategoryBySlug(slug);
        List<StoryDTO> stories = storyService.getStoriesByCategoryPublic(category.getId());
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách truyện theo thể loại thành công", stories));
    }

    /**
     * GET /api/public/categories
     * Lấy tất cả thể loại (kèm slug).
     */
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryDTO>>> getAllCategories() {
        List<CategoryDTO> categoryDTOs = categoryService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thể loại thành công", categoryDTOs));
    }

    /**
     * GET /api/public/categories/{slug}
     * Lấy thông tin một thể loại theo slug SEO của thể loại.
     *
     * @param slug slug của thể loại, ví dụ {@code tien-hiep}
     */
    @GetMapping("/categories/{slug}")
    public ResponseEntity<ApiResponse<CategoryDTO>> getCategoryBySlug(
            @PathVariable String slug) {
        CategoryDTO category = categoryService.getCategoryDTOBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin thể loại thành công", category));
    }

    /**
     * GET /api/public/stories/search?q=...
     * Tìm truyện công khai theo từ khoá text (không phân biệt dấu/hoa thường),
     * khớp trên tiêu đề, slug, thể loại và tác giả.
     *
     * @param q từ khoá tìm kiếm (hỗ trợ thêm tham số {@code keyword} như bí danh)
     */
    @GetMapping("/stories/search")
    public ResponseEntity<ApiResponse<List<StoryDTO>>> searchStories(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "keyword", required = false) String keyword) {
        String term = (q != null && !q.isBlank()) ? q : keyword;
        List<StoryDTO> stories = storyService.searchPublicStories(term);
        return ResponseEntity.ok(ApiResponse.success("Tìm kiếm truyện thành công", stories));
    }

    /**
     * GET /api/public/stories/{slug}
     * Lấy chi tiết một truyện theo slug SEO.
     * Ví dụ: {@code /api/public/stories/dai-quan-gia-la-ma-hoang}.
     *
     * @param slug slug của truyện
     */
    @GetMapping("/stories/{slug}")
    public ResponseEntity<ApiResponse<StoryDTO>> getStoryBySlug(
            @PathVariable String slug) {
        StoryDTO story = storyService.getPublicStoryBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết truyện thành công", story));
    }

    /**
     * GET /api/public/chapters/story/{storySlug}
     * Lấy danh sách chương của truyện (public - không cần authentication), theo slug truyện.
     * Trả về tất cả chương, frontend sẽ check isFree để quyết định hiển thị khóa hay không.
     * User có thể thấy danh sách nhưng chỉ nghe được chương isFree=true hoặc đã mua.
     *
     * NOTE: Nếu user đã đăng nhập và có ownership, tất cả chương sẽ được mở khóa (isFree=true).
     */
    @GetMapping("/chapters/story/{storySlug}")
    public ResponseEntity<ApiResponse<List<ChapterDTO>>> getChaptersByStory(
            @PathVariable String storySlug,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long storyId = storyService.getPublicStoryBySlug(storySlug).getId();
        List<ChapterDTO> chapters = chapterService.getChaptersByStory(storyId, userDetails);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách chương thành công", chapters));
    }
}
