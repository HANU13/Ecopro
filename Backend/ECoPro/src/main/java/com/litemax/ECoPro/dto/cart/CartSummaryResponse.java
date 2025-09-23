package com.litemax.ECoPro.dto.cart;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CartSummaryResponse {
    private Integer itemsCount;
    private Integer uniqueItemsCount;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal shippingAmount;
    private BigDecimal totalAmount;
    private BigDecimal savingsAmount;
    private String couponCode;
    
    // Formatted amounts
    private String formattedSubtotal;
    private String formattedDiscount;
    private String formattedTax;
    private String formattedShipping;
    private String formattedTotal;
    private String formattedSavings;
    
    // Status indicators
    private boolean hasItems;
    private boolean hasDiscounts;
    private boolean hasOutOfStockItems;
    private boolean hasGiftItems;
    private boolean isShippingRequired;
    private boolean freeShippingEligible;
    private BigDecimal freeShippingThreshold;
    private BigDecimal amountForFreeShipping;
    
    // Recommendations
    private List<String> messages;
    private List<CartRecommendation> recommendations;
    
    @Data
    @Builder
    public static class CartRecommendation {
        private String type; // UPSELL, CROSS_SELL, DISCOUNT
        private String title;
        private String description;
        private String actionText;
        private String actionUrl;
    }
}