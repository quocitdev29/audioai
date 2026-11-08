package com.example.audioai.service;

import com.example.audioai.dto.reponse.CategoryDTO;
import com.example.audioai.dto.reponse.StoryDTO;
import com.example.audioai.dto.requests.CreateStoryRequest;
import com.example.audioai.dto.requests.UpdateStoryRequest;
import com.example.audioai.entity.Category;
import com.example.audioai.entity.Story;
import com.example.audioai.entity.User;
import com.example.audioai.exception.ApiException;
import com.example.audioai.repository.CategoryRepository;
import com.example.audioai.repository.ChapterRepository;
import com.example.audioai.repository.OrderRepository;
import com.example.audioai.repository.StoryOwnershipRepository;
import com.example.audioai.repository.StoryRepository;
import com.example.audioai.repository.UserRepository;
import com.example.audioai.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoryService {

    private final StoryRepository storyRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ChapterRepository chapterRepository;
    private final StoryOwnershipRepository storyOwnershipRepository;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public List<StoryDTO> getAllStories() {
        List<Story> stories = storyRepository.findAllByOrderByCreatedAtDesc();
        return stories.stream()
                .map(this::mapToStoryDTO)
                .collect(Collectors.toList());
    }

    /**
     * Danh sách truyện cho trang công khai (trang chủ của user), mới nhất trước.
     *
     * @param status trạng thái cần lọc ({@code null}/rỗng = lấy tất cả trạng thái)
     */
    @Transactional(readOnly = true)
    public List<StoryDTO> getPublicStories(String status) {
        List<Story> stories = (status == null || status.isBlank())
                ? storyRepository.findAllByOrderByCreatedAtDesc()
                : storyRepository.findByStatusOrderByCreatedAtDesc(status);
        return stories.stream()
                .map(this::mapToStoryDTO)
                .collect(Collectors.toList());
    }

    /**
     * Truyện mới nhất (dùng cho section hero ở trang chủ).
     *
     * @param status trạng thái cần lọc ({@code null}/rỗng = lấy tất cả trạng thái)
     * @return StoryDTO hoặc {@code null} nếu chưa có truyện nào
     */
    @Transactional(readOnly = true)
    public StoryDTO getLatestStory(String status) {
        Story story = (status == null || status.isBlank())
                ? storyRepository.findFirstByOrderByCreatedAtDesc().orElse(null)
                : storyRepository.findFirstByStatusOrderByCreatedAtDesc(status).orElse(null);
        return story == null ? null : mapToStoryDTO(story);
    }

    @Transactional(readOnly = true)
    public StoryDTO getStoryById(Long id) {
        Story story = storyRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy truyện"));
        return mapToStoryDTO(story);
    }

    /**
     * Lấy chi tiết truyện theo slug (đường dẫn SEO) cho API công khai.
     * Ví dụ: {@code /api/public/stories/dai-quan-gia-la-ma-hoang}.
     *
     * @param slug slug của truyện
     * @return StoryDTO tương ứng
     * @throws ApiException 400 nếu thiếu slug, 404 nếu không tìm thấy
     */
    @Transactional(readOnly = true)
    public StoryDTO getPublicStoryBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Thiếu thông tin truyện");
        }

        Story story = storyRepository.findBySlug(slug)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy truyện"));
        return mapToStoryDTO(story);
    }

    @Transactional
    public StoryDTO createStory(CreateStoryRequest request, String username) {
        // Validate category exists
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy thể loại"));

        // Get user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));

        // Create story
        Story story = new Story();
        story.setTitle(request.getTitle());
        story.setSlug(generateUniqueSlug(request.getTitle(), null));
        story.setDescription(request.getDescription());
        story.setCoverUrl(request.getCoverUrl());
        story.setPrice(request.getPrice());
        story.setStatus(request.getStatus() != null ? request.getStatus() : "DRAFT");
        story.setCategory(category);
        story.setCreatedBy(user);
        story.setCreatedAt(LocalDateTime.now());
        story.setUpdatedAt(LocalDateTime.now());

        Story savedStory = storyRepository.save(story);
        return mapToStoryDTO(savedStory);
    }

    @Transactional
    public StoryDTO updateStory(Long id, UpdateStoryRequest request, String username) {
        Story story = storyRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy truyện"));

        // Check ownership or admin
        if (!story.getCreatedBy().getUsername().equals(username) && !"admin".equals(username)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền chỉnh sửa truyện này");
        }

        // Update non-null fields
        if (request.getTitle() != null) {
            boolean titleChanged = !request.getTitle().equals(story.getTitle());
            story.setTitle(request.getTitle());
            // Sinh lại slug khi tiêu đề đổi (hoặc truyện cũ chưa có slug)
            if (titleChanged || story.getSlug() == null || story.getSlug().isBlank()) {
                story.setSlug(generateUniqueSlug(request.getTitle(), story.getId()));
            }
        }
        if (request.getDescription() != null) {
            story.setDescription(request.getDescription());
        }
        if (request.getCoverUrl() != null) {
            story.setCoverUrl(request.getCoverUrl());
        }
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy thể loại"));
            story.setCategory(category);
        }
        if (request.getPrice() != null) {
            story.setPrice(request.getPrice());
        }
        if (request.getStatus() != null) {
            story.setStatus(request.getStatus());
        }

        story.setUpdatedAt(LocalDateTime.now());

        Story updatedStory = storyRepository.save(story);
        return mapToStoryDTO(updatedStory);
    }

    @Transactional
    public void deleteStory(Long id, String username) {
        Story story = storyRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy truyện"));

        // Check ownership or admin
        if (!story.getCreatedBy().getUsername().equals(username) && !"admin".equals(username)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền xóa truyện này");
        }

        // Phải xóa các bản ghi con trước khi xóa truyện, nếu không MySQL sẽ chặn
        // bởi khóa ngoại (chapters/orders/story_ownerships đều tham chiếu stories.id).
        // Thứ tự: story_ownerships -> orders (story_ownerships.order_id trỏ tới orders)
        //         -> chapters -> stories.
        int ownerships = storyOwnershipRepository.deleteAllByStoryId(id);
        int orders = orderRepository.deleteAllByStoryId(id);
        int chapters = chapterRepository.deleteAllByStoryId(id);

        storyRepository.delete(story);
        storyRepository.flush();

        log.info("Đã xóa truyện id={} cùng {} chương, {} đơn hàng, {} lượt sở hữu",
                id, chapters, orders, ownerships);
    }

    @Transactional(readOnly = true)
    public List<StoryDTO> getStoriesByCategory(Long categoryId) {
        List<Story> stories = storyRepository.findByCategoryId(categoryId);
        return stories.stream()
                .map(this::mapToStoryDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách truyện theo thể loại cho trang công khai (chỉ truyện PUBLISHED).
     *
     * @param categoryId ID của thể loại
     * @return danh sách truyện thuộc thể loại đó, sắp xếp mới nhất trước
     */
    @Transactional(readOnly = true)
    public List<StoryDTO> getStoriesByCategoryPublic(Long categoryId) {
        // Kiểm tra category có tồn tại không
        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy thể loại"));
        
        List<Story> stories = storyRepository.findByCategoryId(categoryId);
        // Lọc chỉ lấy truyện PUBLISHED và sắp xếp mới nhất trước
        return stories.stream()
                .filter(story -> "PUBLISHED".equals(story.getStatus()))
                .sorted((s1, s2) -> s2.getCreatedAt().compareTo(s1.getCreatedAt()))
                .map(this::mapToStoryDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StoryDTO> getStoriesByStatus(String status) {
        List<Story> stories = storyRepository.findByStatus(status);
        return stories.stream()
                .map(this::mapToStoryDTO)
                .collect(Collectors.toList());
    }

    /**
     * Tìm truyện công khai (PUBLISHED) theo từ khoá text, không phân biệt dấu và
     * hoa/thường. Khớp trên tiêu đề, slug, tên thể loại (kèm slug thể loại) và
     * tên/username người đăng.
     *
     * @param keyword từ khoá người dùng nhập (có thể có dấu)
     * @return danh sách truyện khớp, mới nhất trước; rỗng nếu keyword trống
     */
    @Transactional(readOnly = true)
    public List<StoryDTO> searchPublicStories(String keyword) {
        String needle = SlugUtil.normalizeForSearch(keyword);
        if (needle.isBlank()) {
            return List.of();
        }

        return storyRepository.findByStatusOrderByCreatedAtDesc("PUBLISHED").stream()
                .filter(story -> buildSearchText(story).contains(needle))
                .map(this::mapToStoryDTO)
                .collect(Collectors.toList());
    }

    /**
     * Ghép các trường có thể tìm kiếm của truyện thành một chuỗi đã bỏ dấu.
     */
    private String buildSearchText(Story story) {
        StringBuilder sb = new StringBuilder();
        appendSearchTerm(sb, story.getTitle());
        appendSearchTerm(sb, story.getSlug());
        if (story.getCategory() != null) {
            appendSearchTerm(sb, story.getCategory().getName());
            appendSearchTerm(sb, story.getCategory().getSlug());
        }
        if (story.getCreatedBy() != null) {
            appendSearchTerm(sb, story.getCreatedBy().getHovaten());
            appendSearchTerm(sb, story.getCreatedBy().getUsername());
        }
        return SlugUtil.normalizeForSearch(sb.toString());
    }

    private void appendSearchTerm(StringBuilder sb, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(' ').append(value);
        }
    }

    /**
     * Backfill slug cho toàn bộ truyện cũ chưa có slug (gọi lúc khởi động ứng dụng).
     * Bỏ qua những truyện đã có slug để không làm thay đổi URL đang tồn tại.
     *
     * @return số truyện đã được cập nhật
     */
    @Transactional
    public int backfillSlugs() {
        List<Story> stories = storyRepository.findAll();
        int updated = 0;
        for (Story story : stories) {
            if (story.getSlug() == null || story.getSlug().isBlank()) {
                story.setSlug(generateUniqueSlug(story.getTitle(), story.getId()));
                storyRepository.save(story);
                updated++;
            }
        }
        if (updated > 0) {
            log.info("Đã sinh slug cho {} truyện cũ", updated);
        }
        return updated;
    }

    /**
     * Sinh slug duy nhất từ tiêu đề. Nếu slug đã tồn tại (của truyện khác) thì
     * thêm hậu tố {@code -2}, {@code -3}, ... cho tới khi không trùng.
     *
     * @param title          tiêu đề truyện
     * @param excludeStoryId id truyện đang cập nhật (bỏ qua khi kiểm tra trùng),
     *                       truyền {@code null} khi tạo mới
     * @return slug duy nhất
     */
    private String generateUniqueSlug(String title, Long excludeStoryId) {
        String base = SlugUtil.toSlug(title);
        if (base.isBlank()) {
            base = "truyen";
        }

        String candidate = base;
        int suffix = 2;
        while (true) {
            Optional<Story> existing = storyRepository.findBySlug(candidate);
            if (existing.isEmpty()
                    || (excludeStoryId != null && excludeStoryId.equals(existing.get().getId()))) {
                return candidate;
            }
            candidate = base + "-" + suffix++;
        }
    }

    private StoryDTO mapToStoryDTO(Story story) {
        return StoryDTO.builder()
                .id(story.getId())
                .title(story.getTitle())
                .slug(story.getSlug())
                .description(story.getDescription())
                .coverUrl(story.getCoverUrl())
                .price(story.getPrice())
                .status(story.getStatus())
                .createdAt(story.getCreatedAt())
                .updatedAt(story.getUpdatedAt())
                .chapterCount(chapterRepository.countByStoryId(story.getId()))
                .category(CategoryDTO.builder()
                        .id(story.getCategory().getId())
                        .name(story.getCategory().getName())
                        .slug(story.getCategory().getSlug())
                        .build())
                .createdBy(StoryDTO.CreatedByDTO.builder()
                        .id(story.getCreatedBy().getId())
                        .username(story.getCreatedBy().getUsername())
                        .hovaten(story.getCreatedBy().getHovaten())
                        .build())
                .build();
    }
}
