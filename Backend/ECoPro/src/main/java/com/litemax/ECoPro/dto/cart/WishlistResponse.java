package com.litemax.ECoPro.dto.cart;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class WishlistResponse {
    private Long id;
    private Long userId;
    private Long productId;
    private String productName;
    private String productSlug;
    private Long variantId;
    private String variantName;
    private String displayName;
    private String notes;
    private Boolean isPublic;
    private Integer priority;
    private String priorityText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Product details
    private BigDecimal price;
    private BigDecimal comparePrice;
    private String imageUrl;
    private boolean inStock;
    private boolean available;
    private String stockStatus;
    private String productUrl;
    
    // Additional fields
    private String formattedPrice;
    private String formattedComparePrice;
    private boolean hasDiscount;
    private BigDecimal discountPercentage;
    private boolean canAddToCart;
}