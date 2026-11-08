package com.example.audioai.repository;

import com.example.audioai.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {

    /** Danh sách chương của một truyện, sắp theo số chương tăng dần. */
    List<Chapter> findByStoryIdOrderByChapterNumberAsc(Long storyId);

    /** Tất cả chương, gom theo truyện rồi theo số chương. */
    List<Chapter> findAllByOrderByStoryIdAscChapterNumberAsc();

    /** Đếm số chương của một truyện. */
    long countByStoryId(Long storyId);

    /** Kiểm tra trùng số chương trong cùng một truyện. */
    boolean existsByStoryIdAndChapterNumber(Long storyId, Integer chapterNumber);

    /** Xóa toàn bộ chương của một truyện (dùng khi xóa truyện). */
    @Modifying
    @Query(value = "DELETE FROM chapters WHERE story_id = :storyId", nativeQuery = true)
    int deleteAllByStoryId(@Param("storyId") Long storyId);
}