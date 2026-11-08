package com.example.audioai.repository;

import com.example.audioai.entity.Story;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StoryRepository extends JpaRepository<Story, Long> {
    List<Story> findAllByOrderByCreatedAtDesc();

    /** Truyen theo slug (duong dan SEO) - dung cho URL /story/{slug}. */
    Optional<Story> findBySlug(String slug);

    /** Kiem tra slug da ton tai chua (tranh trung khi sinh slug tu title). */
    boolean existsBySlug(String slug);
    
    @Query("SELECT s FROM Story s WHERE s.category.id = :categoryId")
    List<Story> findByCategoryId(@Param("categoryId") Long categoryId);
    
    List<Story> findByStatus(String status);

    /** Truyen theo trang thai, truyen moi nhat truoc (dung cho API cong khai). */
    List<Story> findByStatusOrderByCreatedAtDesc(String status);

    /** Truyen moi nhat (moi trang thai) - hien thi o hero trang chu. */
    Optional<Story> findFirstByOrderByCreatedAtDesc();

    /** Truyen moi nhat theo trang thai - hien thi o hero trang chu. */
    Optional<Story> findFirstByStatusOrderByCreatedAtDesc(String status);
}
