package com.example.audioai.repository;

import com.example.audioai.entity.StoryOwnership;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoryOwnershipRepository extends JpaRepository<StoryOwnership, Long> {
    
    boolean existsByUserIdAndStoryId(Long userId, Long storyId);
    
    Optional<StoryOwnership> findByUserIdAndStoryId(Long userId, Long storyId);
    
    @Query("SELECT so FROM StoryOwnership so " +
           "LEFT JOIN FETCH so.story s " +
           "LEFT JOIN FETCH s.category " +
           "WHERE so.userId = :userId " +
           "ORDER BY so.purchasedAt DESC")
    Page<StoryOwnership> findByUserIdWithStory(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT so FROM StoryOwnership so " +
           "LEFT JOIN FETCH so.story s " +
           "LEFT JOIN FETCH so.order o " +
           "WHERE so.userId = :userId " +
           "ORDER BY so.purchasedAt DESC")
    List<StoryOwnership> findByUserId(@Param("userId") Long userId);

    /** Xóa toàn bộ quyền sở hữu của một truyện (dùng khi xóa truyện). */
    @Modifying
    @Query(value = "DELETE FROM story_ownerships WHERE story_id = :storyId", nativeQuery = true)
    int deleteAllByStoryId(@Param("storyId") Long storyId);
}
