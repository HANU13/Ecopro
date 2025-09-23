package com.litemax.ECoPro.controller.product;

import lombok.Data;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Data
public class RefundRequest {
    
    @NotNull(message = "Gateway is required")
    private PaymentGatewayRequest.PaymentGateway gateway;
    
    @NotBlank(message = "Original transaction ID is required")
    private String originalTransactionId;
    
    @DecimalMin(value = "0.01", message = "Refund amount must be greater than 0")
    private BigDecimal amount; // null for full refund
    
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    private String currency = "USD";
    
    private String reason;
}