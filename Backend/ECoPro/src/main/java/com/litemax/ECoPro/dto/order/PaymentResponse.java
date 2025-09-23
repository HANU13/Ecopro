package com.litemax.ECoPro.dto.order;

import com.litemax.ECoPro.entity.order.Payment.PaymentMethod;
import com.litemax.ECoPro.entity.order.Payment.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentResponse {
    private Long id;
    private String paymentReference;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private BigDecimal amount;
    private String gatewayTransactionId;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}