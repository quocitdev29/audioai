package com.example.audioai.service;

import com.example.audioai.dto.reponse.CategoryDTO;
import com.example.audioai.entity.Category;
import com.example.audioai.exception.ApiException;
import com.example.audioai.repository.CategoryRepository;
import com.example.audioai.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service xử lý thể loại (category) công khai: trả DTO kèm slug,
 * tra cứu theo id hoặc slug và sinh slug cho dữ liệu cũ.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * Lấy tất cả thể loại dưới dạng DTO (kèm slug để frontend dựng URL).
     */
    @Transactional(readOnly = true)
    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Tra cứu thể loại theo slug (đường dẫn SEO).
     * Ví dụ: {@code tien-hiep}.
     *
     * @param slug slug của thể loại
     * @return entity Category
     * @throws ApiException 400 nếu thiếu slug, 404 nếu không tìm thấy
     */
    @Transactional(readOnly = true)
    public Category getCategoryBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Thiếu thông tin thể loại");
        }

        return categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy thể loại"));
    }

    /**
     * Tra cứu thể loại theo slug và trả về DTO (kèm slug) cho frontend.
     *
     * @param slug slug của thể loại
     * @return CategoryDTO
     * @throws ApiException 404 nếu không tìm thấy
     */
    @Transactional(readOnly = true)
    public CategoryDTO getCategoryDTOBySlug(String slug) {
        return mapToDTO(getCategoryBySlug(slug));
    }

    /**
     * Backfill slug cho thể loại cũ chưa có slug (gọi lúc khởi động ứng dụng).
     *
     * @return số thể loại đã được cập nhật
     */
    @Transactional
    public int backfillSlugs() {
        List<Category> categories = categoryRepository.findAll();
        int updated = 0;
        for (Category category : categories) {
            if (category.getSlug() == null || category.getSlug().isBlank()) {
                category.setSlug(generateUniqueSlug(category.getName(), category.getId()));
                categoryRepository.save(category);
                updated++;
            }
        }
        if (updated > 0) {
            log.info("Đã sinh slug cho {} thể loại", updated);
        }
        return updated;
    }

    /**
     * Sinh slug duy nhất từ tên thể loại (thêm hậu tố -2, -3... nếu trùng).
     */
    private String generateUniqueSlug(String name, Long excludeCategoryId) {
        String base = SlugUtil.toSlug(name);
        if (base.isBlank()) {
            base = "the-loai";
        }

        String candidate = base;
        int suffix = 2;
        while (true) {
            Optional<Category> existing = categoryRepository.findBySlug(candidate);
            if (existing.isEmpty()
                    || (excludeCategoryId != null && excludeCategoryId.equals(existing.get().getId()))) {
                return candidate;
            }
            candidate = base + "-" + suffix++;
        }
    }

    private CategoryDTO mapToDTO(Category category) {
        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .build();
    }
}
