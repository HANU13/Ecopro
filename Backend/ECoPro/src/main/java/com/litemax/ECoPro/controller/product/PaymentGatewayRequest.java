package com.litemax.ECoPro.controller.product;

import lombok.Data;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Data
public class PaymentGatewayRequest {
    
    @NotNull(message = "Gateway is required")
    private PaymentGateway gateway;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    private String currency = "USD";
    
    @NotBlank(message = "Payment method ID is required")
    private String paymentMethodId;
    
    private String customerId;
    private String returnUrl;
    private Long userId;
    private String description;
    
    public enum PaymentGateway {
        STRIPE, PAYPAL
    }
}
