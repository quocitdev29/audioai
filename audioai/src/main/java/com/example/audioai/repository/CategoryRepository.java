package com.example.audioai.repository;

import com.example.audioai.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    /** The loai theo slug (duong dan SEO) - dung cho URL /category/{slug}. */
    Optional<Category> findBySlug(String slug);

    /** Kiem tra slug da ton tai chua (tranh trung khi sinh slug tu name). */
    boolean existsBySlug(String slug);
}
