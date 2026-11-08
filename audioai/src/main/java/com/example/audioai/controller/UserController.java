package com.example.audioai.controller;

import com.example.audioai.dto.OwnershipCheckResponse;
import com.example.audioai.dto.PurchaseStoryRequest;
import com.example.audioai.dto.PurchaseStoryResponse;
import com.example.audioai.dto.reponse.ApiResponse;
import com.example.audioai.entity.User;
import com.example.audioai.repository.UserRepository;
import com.example.audioai.service.OrderService;
import com.example.audioai.service.StoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller cho các endpoint của user (yêu cầu authentication).
 * Bao gồm: mua truyện, lịch sử mua, kiểm tra ownership.
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final OrderService orderService;
    private final UserRepository userRepository;
    private final StoryService storyService;

    /**
     * POST /api/user/orders/purchase-story
     * Mua truyện bằng token trong ví.
     */
    @PostMapping("/orders/purchase-story")
    public ResponseEntity<ApiResponse<PurchaseStoryResponse>> purchaseStory(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PurchaseStoryRequest request) {
        
        try {
            User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

            PurchaseStoryResponse response = orderService.purchaseStory(user.getId(), request.getStoryId());

            return ResponseEntity.ok(ApiResponse.<PurchaseStoryResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Mua truyện thành công")
                .data(response)
                .build());

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.<PurchaseStoryResponse>builder()
                .success(false)
                .code(HttpStatus.BAD_REQUEST.value())
                .message(e.getMessage())
                .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<PurchaseStoryResponse>builder()
                    .success(false)
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("Có lỗi xảy ra: " + e.getMessage())
                    .build());
        }
    }

    /**
     * GET /api/user/orders/my-purchases
     * Lấy danh sách truyện đã mua của user hiện tại.
     */
    @GetMapping("/orders/my-purchases")
    public ResponseEntity<ApiResponse<List<PurchaseStoryResponse>>> getMyPurchases(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

            List<PurchaseStoryResponse> purchases = orderService.getMyPurchases(user.getId());

            return ResponseEntity.ok(ApiResponse.<List<PurchaseStoryResponse>>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách truyện đã mua thành công")
                .data(purchases)
                .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<List<PurchaseStoryResponse>>builder()
                    .success(false)
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("Có lỗi xảy ra: " + e.getMessage())
                    .build());
        }
    }

    /**
     * GET /api/user/story-ownerships/check/{storySlug}
     * Kiểm tra xem user hiện tại có sở hữu truyện hay không (tra truyện theo slug).
     */
    @GetMapping("/story-ownerships/check/{storySlug}")
    public ResponseEntity<ApiResponse<OwnershipCheckResponse>> checkOwnership(
            @PathVariable String storySlug,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

            Long storyId = storyService.getPublicStoryBySlug(storySlug).getId();
            OwnershipCheckResponse response = orderService.checkOwnership(user.getId(), storyId);

            return ResponseEntity.ok(ApiResponse.<OwnershipCheckResponse>builder()
                .success(true)
                .code(HttpStatus.OK.value())
                .message("Success")
                .data(response)
                .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<OwnershipCheckResponse>builder()
                    .success(false)
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("Có lỗi xảy ra: " + e.getMessage())
                    .build());
        }
    }
}
