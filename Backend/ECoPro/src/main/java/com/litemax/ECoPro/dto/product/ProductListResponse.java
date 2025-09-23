package com.litemax.ECoPro.dto.product;

import com.litemax.ECoPro.entity.product.Product;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ProductListResponse {
    private Long id;
    private String name;
    private String slug;
    private String shortDescription;
    private String sku;
    private BigDecimal price;
    private BigDecimal comparePrice;
    private Product.ProductStatus status;
    private boolean featured;
    private Long viewCount;
    private BigDecimal rating;
    private Integer reviewCount;
    private LocalDateTime createdAt;

    // Essential relationships
    private String sellerName;
    private String brandName;
    private List<String> categoryNames;
    private String primaryImageUrl;

    // Computed fields
    private boolean inStock;
    private BigDecimal discountPercentage;
    private boolean hasDiscount;
    private String stockStatus;
    private String formattedPrice;
    private String formattedComparePrice;
}