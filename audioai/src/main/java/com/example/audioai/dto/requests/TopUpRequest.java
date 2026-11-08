package com.example.audioai.dto.requests;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Yêu cầu nạp tiền vào ví qua VNPay.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopUpRequest {

    @NotNull(message = "Số tiền nạp không được để trống")
    @DecimalMin(value = "10000", message = "Số tiền nạp tối thiểu là 10.000 VNĐ")
    @DecimalMax(value = "100000000", message = "Số tiền nạp tối đa là 100.000.000 VNĐ")
    @Digits(integer = 9, fraction = 2, message = "Số tiền nạp không hợp lệ")
    private BigDecimal amount;

    @Size(max = 200, message = "Nội dung nạp tiền không được vượt quá 200 ký tự")
    private String description;
}
