package com.example.audioai.service;

import com.example.audioai.dto.reponse.AdminStatsResponse;
import com.example.audioai.dto.reponse.PaymentHistoryResponse;
import com.example.audioai.entity.Payment;
import com.example.audioai.entity.User;
import com.example.audioai.exception.ApiException;
import com.example.audioai.repository.PaymentRepository;
import com.example.audioai.repository.TransactionHistoryRepository;
import com.example.audioai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final TransactionHistoryRepository transactionRepository;
    private final PaymentRepository paymentRepository;

    /**
     * Lấy thống kê tổng quan cho admin dashboard
     */
    public AdminStatsResponse getStatistics() {
        // Tổng số người dùng (không tính admin)
        Long totalUsers = userRepository.count() - 1; // -1 để loại admin

        // Tổng số đơn hàng (để sau)
        Long totalOrders = 0L;

        // Tổng doanh thu = số dư của admin
        User admin = userRepository.findByUsername("admin")
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản admin"));
        BigDecimal totalRevenue = admin.getBalance();

        // Tổng số giao dịch
        Long totalTransactions = transactionRepository.count();

        return AdminStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalOrders(totalOrders)
                .totalRevenue(totalRevenue)
                .totalTransactions(totalTransactions)
                .build();
    }

    /**
     * Lấy danh sách lịch sử thanh toán từ bảng payments
     */
    public List<PaymentHistoryResponse> getPaymentHistory(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Payment> payments = paymentRepository.findAll(pageable);

        return payments.getContent().stream()
                .map(this::mapToPaymentHistoryResponse)
                .collect(Collectors.toList());
    }

    /**
     * Map Payment sang PaymentHistoryResponse
     */
    private PaymentHistoryResponse mapToPaymentHistoryResponse(Payment payment) {
        User user = payment.getUser();
        
        return PaymentHistoryResponse.builder()
                .id(payment.getId())
                .username(user.getUsername())
                .hovaten(user.getHovaten())
                .type(payment.getMethod()) // VNPAY
                .amount(payment.getAmount())
                .status(payment.getStatus()) // SUCCESS, FAILED, PENDING
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
