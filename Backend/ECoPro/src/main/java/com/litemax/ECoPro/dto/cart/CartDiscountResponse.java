package com.litemax.ECoPro.dto.cart;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CartDiscountResponse {
    private Long id;
    private String discountCode;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal discountAmount;
    private String title;
    private String description;
    private LocalDateTime createdAt;
    
    // Additional fields
    private String formattedDiscountAmount;
    private boolean isPercentage;
    private String displayText;
}