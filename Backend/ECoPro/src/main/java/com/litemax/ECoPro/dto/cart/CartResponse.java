package com.litemax.ECoPro.dto.cart;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CartResponse {
    private Long id;
    private Long userId;
    private String sessionId;
    private String status;
    private Integer itemsCount;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal savingsAmount;
    private String couponCode;
    private String notes;
    private LocalDateTime expiresAt;
    private List<CartItemResponse> items;
    private List<CartDiscountResponse> discounts;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Additional fields
    private boolean isEmpty;
    private boolean isExpired;
    private String formattedSubtotal;
    private String formattedTotal;
    private String formattedSavings;
    private CartSummary summary;
    
    @Data
    @Builder
    public static class CartSummary {
        private int totalItems;
        private int uniqueItems;
        private BigDecimal averageItemPrice;
        private boolean hasOutOfStockItems;
        private boolean hasGiftItems;
        private String estimatedDelivery;
    }
}