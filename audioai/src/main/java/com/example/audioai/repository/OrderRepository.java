package com.example.audioai.repository;

import com.example.audioai.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    Optional<Order> findByOrderNumber(String orderNumber);
    
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    boolean existsByUserIdAndStoryId(Long userId, Long storyId);

    /** Xóa toàn bộ đơn hàng của một truyện (dùng khi xóa truyện). */
    @Modifying
    @Query(value = "DELETE FROM orders WHERE story_id = :storyId", nativeQuery = true)
    int deleteAllByStoryId(@Param("storyId") Long storyId);
}
