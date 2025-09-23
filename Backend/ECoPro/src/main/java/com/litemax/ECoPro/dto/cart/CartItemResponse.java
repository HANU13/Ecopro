package com.litemax.ECoPro.dto.cart;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CartItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productSlug;
    private Long variantId;
    private String variantName;
    private String displayName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal comparePrice;
    private BigDecimal totalPrice;
    private BigDecimal savingsAmount;
    private String imageUrl;
    private String customAttributes;
    private Boolean giftWrap;
    private String giftMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Additional fields
    private boolean inStock;
    private boolean available;
    private Integer availableQuantity;
    private String stockStatus;
    private String formattedUnitPrice;
    private String formattedTotalPrice;
    private String formattedSavings;
    private boolean hasDiscount;
    private String productUrl;
}