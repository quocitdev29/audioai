package com.example.audioai.service;

import com.example.audioai.dto.OwnershipCheckResponse;
import com.example.audioai.dto.PurchaseStoryResponse;
import com.example.audioai.entity.*;
import com.example.audioai.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final StoryOwnershipRepository ownershipRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final StoryRepository storyRepository;

    @Transactional
    public PurchaseStoryResponse purchaseStory(Long userId, Long storyId) {
        // 1. Validate user
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Validate story
        Story story = storyRepository.findById(storyId)
            .orElseThrow(() -> new RuntimeException("Story not found"));

        // 3. Check already owned
        if (ownershipRepository.existsByUserIdAndStoryId(userId, storyId)) {
            throw new RuntimeException("Bạn đã sở hữu truyện này");
        }

        // 4. Check balance
        BigDecimal storyPrice = story.getPrice() != null ? story.getPrice() : BigDecimal.ZERO;
        BigDecimal currentBalance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        
        if (currentBalance.compareTo(storyPrice) < 0) {
            BigDecimal shortage = storyPrice.subtract(currentBalance);
            throw new RuntimeException(String.format(
                "Số dư không đủ để mua truyện này. Cần thêm %s Xu",
                shortage.toString()
            ));
        }

        // 5. Create order
        String orderNumber = generateOrderNumber();
        Order order = new Order();
        order.setUserId(userId);
        order.setStoryId(storyId);
        order.setOrderNumber(orderNumber);
        order.setAmount(storyPrice);
        order.setStatus(Order.OrderStatus.PENDING);
        order.setPaymentMethod("WALLET");
        order = orderRepository.save(order);

        try {
            // 6. Deduct balance
            BigDecimal newBalance = currentBalance.subtract(storyPrice);
            user.setBalance(newBalance);
            userRepository.save(user);

            // 7. Create transaction
            String transactionNumber = generateTransactionNumber();
            Transaction transaction = new Transaction();
            transaction.setUserId(userId);
            transaction.setTransactionNumber(transactionNumber);
            transaction.setType(Transaction.TransactionType.PURCHASE);
            transaction.setAmount(storyPrice.negate()); // Negative for purchase
            transaction.setBalanceBefore(currentBalance);
            transaction.setBalanceAfter(newBalance);
            transaction.setDescription("Mua truyện: " + story.getTitle());
            transaction.setReferenceType("ORDER");
            transaction.setReferenceId(order.getId());
            transaction.setStatus(Transaction.TransactionStatus.SUCCESS);
            transactionRepository.save(transaction);

            // 8. Create ownership
            StoryOwnership ownership = new StoryOwnership();
            ownership.setUserId(userId);
            ownership.setStoryId(storyId);
            ownership.setOrderId(order.getId());
            ownership.setPurchasedPrice(storyPrice);
            ownership.setPurchasedAt(LocalDateTime.now());
            ownershipRepository.save(ownership);

            // 9. Update order status
            order.setStatus(Order.OrderStatus.COMPLETED);
            orderRepository.save(order);

            // 10. Return response
            return PurchaseStoryResponse.builder()
                .orderId(order.getId())
                .orderNumber(orderNumber)
                .storyId(storyId)
                .storyTitle(story.getTitle())
                .amount(storyPrice)
                .status(order.getStatus().name())
                .purchasedAt(ownership.getPurchasedAt())
                .newBalance(newBalance)
                .build();

        } catch (Exception e) {
            // Rollback handled by @Transactional
            order.setStatus(Order.OrderStatus.FAILED);
            orderRepository.save(order);
            throw new RuntimeException("Có lỗi xảy ra khi mua truyện: " + e.getMessage());
        }
    }

    public OwnershipCheckResponse checkOwnership(Long userId, Long storyId) {
        return ownershipRepository.findByUserIdAndStoryId(userId, storyId)
            .map(ownership -> OwnershipCheckResponse.builder()
                .owned(true)
                .purchasedAt(ownership.getPurchasedAt())
                .orderNumber(ownership.getOrder() != null ? 
                    ownership.getOrder().getOrderNumber() : null)
                .build())
            .orElse(OwnershipCheckResponse.builder()
                .owned(false)
                .build());
    }

    public List<PurchaseStoryResponse> getMyPurchases(Long userId) {
        List<StoryOwnership> ownerships = ownershipRepository.findByUserId(userId);
        
        return ownerships.stream()
            .map(ownership -> {
                Story story = ownership.getStory();
                Order order = ownership.getOrder();
                
                return PurchaseStoryResponse.builder()
                    .orderId(order != null ? order.getId() : null)
                    .orderNumber(order != null ? order.getOrderNumber() : null)
                    .storyId(story.getId())
                    .storyTitle(story.getTitle())
                    .amount(ownership.getPurchasedPrice())
                    .status(order != null ? order.getStatus().name() : "COMPLETED")
                    .purchasedAt(ownership.getPurchasedAt())
                    .newBalance(null) // Not applicable for list view
                    .build();
            })
            .collect(Collectors.toList());
    }

    private String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        );
        return "ORD_" + timestamp;
    }

    private String generateTransactionNumber() {
        String timestamp = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        );
        return "TXN_" + timestamp;
    }
}
