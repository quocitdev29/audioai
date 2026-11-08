package com.example.audioai.service;

import com.example.audioai.dto.reponse.PaymentResultResponse;
import com.example.audioai.dto.reponse.PaymentUrlResponse;
import com.example.audioai.dto.requests.TopUpRequest;
import com.example.audioai.entity.Payment;
import com.example.audioai.entity.TransactionHistory;
import com.example.audioai.entity.User;
import com.example.audioai.exception.ApiException;
import com.example.audioai.repository.PaymentRepository;
import com.example.audioai.repository.TransactionHistoryRepository;
import com.example.audioai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Nghiệp vụ nạp tiền qua VNPay: tạo giao dịch chờ, cập nhật số dư và lưu lịch sử giao dịch.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    public static final String METHOD_VNPAY = "VNPAY";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String TYPE_TOPUP = "TOPUP";
    public static final String VNP_SUCCESS_CODE = "00";

    private final PaymentRepository paymentRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final UserRepository userRepository;
    private final VnPayService vnPayService;

    /**
     * Tạo giao dịch nạp tiền ở trạng thái PENDING cho đúng người đang đăng nhập
     * và trả về URL thanh toán VNPay.
     */
    @Transactional
    public PaymentUrlResponse createTopUpPayment(String username, TopUpRequest request, String ipAddress) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));

        String transactionCode = generateTransactionCode();
        BigDecimal amount = request.getAmount();

        Payment payment = new Payment();
        payment.setUser(user);
        payment.setMethod(METHOD_VNPAY);
        payment.setTransactionCode(transactionCode);
        payment.setAmount(amount);
        payment.setStatus(STATUS_PENDING);
        payment.setCreatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        String orderInfo = "Nap tien vao tai khoan " + user.getUsername();
        String paymentUrl = vnPayService.createPaymentUrl(transactionCode, amount, orderInfo, ipAddress);

        return PaymentUrlResponse.builder()
                .transactionCode(transactionCode)
                .amount(amount)
                .paymentUrl(paymentUrl)
                .build();
    }

    /**
     * Xử lý kết quả VNPay trả về qua ReturnUrl (trình duyệt) hoặc IPN.
     * Cộng tiền cho đúng user đã tạo giao dịch, có chống cộng trùng.
     */
    @Transactional
    public PaymentResultResponse processVnPayCallback(Map<String, String> params) {
        String transactionCode = params.get("vnp_TxnRef");
        Payment payment = paymentRepository.findByTransactionCode(transactionCode)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy giao dịch với mã: " + transactionCode));
        return applyResult(payment, params);
    }

    /**
     * Xử lý IPN (server-to-server) theo đúng bộ mã phản hồi VNPAY yêu cầu.
     */
    @Transactional
    public Map<String, String> processIpn(Map<String, String> params) {
        String transactionCode = params.get("vnp_TxnRef");
        Optional<Payment> optionalPayment = paymentRepository.findByTransactionCode(transactionCode);

        if (optionalPayment.isEmpty()) {
            return ipnResponse("01", "Order not found");
        }

        Payment payment = optionalPayment.get();
        if (!STATUS_PENDING.equals(payment.getStatus())) {
            return ipnResponse("02", "Order already confirmed");
        }
        if (!amountMatches(payment, params)) {
            return ipnResponse("04", "Invalid amount");
        }

        applyResult(payment, params);
        return ipnResponse("00", "Confirm Success");
    }

    private PaymentResultResponse applyResult(Payment payment, Map<String, String> params) {
        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");
        boolean success = VNP_SUCCESS_CODE.equals(responseCode) && VNP_SUCCESS_CODE.equals(transactionStatus);

        // Đã xử lý trước đó -> trả lại kết quả cũ (chống cộng tiền 2 lần)
        if (!STATUS_PENDING.equals(payment.getStatus())) {
            return toResult(payment, params, vnPayService.getResponseMessage(responseCode));
        }

        User user = payment.getUser();

        if (success) {
            BigDecimal balanceBefore = balanceOf(user);
            BigDecimal balanceAfter = balanceBefore.add(payment.getAmount());

            // Cộng tiền cho user
            user.setBalance(balanceAfter);
            userRepository.save(user);

            payment.setStatus(STATUS_SUCCESS);
            paymentRepository.save(payment);

            // Tạo lịch sử giao dịch cho user
            TransactionHistory history = new TransactionHistory();
            history.setTransactionNumber(generateTransactionNumber());
            history.setUser(user);
            history.setType(TYPE_TOPUP);
            history.setAmount(payment.getAmount());
            history.setBalanceBefore(balanceBefore);
            history.setBalanceAfter(balanceAfter);
            history.setReferenceId(payment.getId());
            history.setDescription(buildDescription(params));
            history.setCreatedAt(LocalDateTime.now());
            transactionHistoryRepository.save(history);

            // Cộng tiền vào tài khoản admin
            addRevenueToAdmin(payment.getAmount(), payment.getId());
        } else {
            payment.setStatus(STATUS_FAILED);
            paymentRepository.save(payment);
        }

        return toResult(payment, params, vnPayService.getResponseMessage(responseCode));
    }

    /**
     * Cộng doanh thu vào tài khoản admin.
     */
    private void addRevenueToAdmin(BigDecimal amount, Long paymentId) {
        try {
            userRepository.findByUsername("admin").ifPresent(admin -> {
                BigDecimal adminBalanceBefore = balanceOf(admin);
                BigDecimal adminBalanceAfter = adminBalanceBefore.add(amount);

                admin.setBalance(adminBalanceAfter);
                userRepository.save(admin);

                // Tạo lịch sử giao dịch cho admin
                TransactionHistory adminHistory = new TransactionHistory();
                adminHistory.setTransactionNumber(generateTransactionNumber());
                adminHistory.setUser(admin);
                adminHistory.setType("REVENUE");
                adminHistory.setAmount(amount);
                adminHistory.setBalanceBefore(adminBalanceBefore);
                adminHistory.setBalanceAfter(adminBalanceAfter);
                adminHistory.setReferenceId(paymentId);
                adminHistory.setDescription("Doanh thu tu nap tien user - Payment ID: " + paymentId);
                adminHistory.setCreatedAt(LocalDateTime.now());
                transactionHistoryRepository.save(adminHistory);
            });
        } catch (Exception e) {
            // Log error nhưng không throw exception để không ảnh hưởng đến giao dịch user
            System.err.println("Failed to add revenue to admin: " + e.getMessage());
        }
    }

    private PaymentResultResponse toResult(Payment payment, Map<String, String> params, String message) {
        return PaymentResultResponse.builder()
                .transactionCode(payment.getTransactionCode())
                .vnpTransactionNo(params.get("vnp_TransactionNo"))
                .bankCode(params.get("vnp_BankCode"))
                .responseCode(params.get("vnp_ResponseCode"))
                .transactionStatus(params.get("vnp_TransactionStatus"))
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .balance(balanceOf(payment.getUser()))
                .message(message)
                .build();
    }

    private boolean amountMatches(Payment payment, Map<String, String> params) {
        String vnpAmount = params.get("vnp_Amount");
        if (vnpAmount == null) {
            return false;
        }
        long expected = payment.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
        try {
            return expected == Long.parseLong(vnpAmount);
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private String buildDescription(Map<String, String> params) {
        StringBuilder sb = new StringBuilder("Nap tien qua VNPAY");
        String vnpTransactionNo = params.get("vnp_TransactionNo");
        String bankCode = params.get("vnp_BankCode");
        String responseCode = params.get("vnp_ResponseCode");

        if (vnpTransactionNo != null && !vnpTransactionNo.isBlank()) {
            sb.append(" - GD: ").append(vnpTransactionNo);
        }
        if (bankCode != null && !bankCode.isBlank()) {
            sb.append(" - NH: ").append(bankCode);
        }
        sb.append(" - Ma phan hoi: ").append(responseCode);
        return sb.toString();
    }

    private BigDecimal balanceOf(User user) {
        return user.getBalance() == null ? BigDecimal.ZERO : user.getBalance();
    }

    private Map<String, String> ipnResponse(String rspCode, String message) {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("RspCode", rspCode);
        response.put("Message", message);
        return response;
    }

    private String generateTransactionCode() {
        return "PAY" + System.currentTimeMillis()
                + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
    }

    private String generateTransactionNumber() {
        return "TXN" + System.currentTimeMillis()
                + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
    }
}

