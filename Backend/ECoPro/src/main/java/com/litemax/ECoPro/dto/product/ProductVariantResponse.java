package com.litemax.ECoPro.dto.product;

import com.litemax.ECoPro.entity.product.ProductVariant;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ProductVariantResponse {
    private Long id;
    private String name;
    private String sku;
    private String option1Name;
    private String option1Value;
    private String option2Name;
    private String option2Value;
    private String option3Name;
    private String option3Value;
    private BigDecimal price;
    private BigDecimal comparePrice;
    private BigDecimal costPrice;
    private Integer inventoryQuantory;
    private ProductVariant.InventoryPolicy inventoryPolicy;
    private BigDecimal weight;
    private String barcode;
    private String imageUrl;
    private boolean active;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Computed fields
    private String displayName;
    private boolean inStock;
    private BigDecimal discountPercentage;
    private String formattedPrice;
    private String formattedComparePrice;
}