package com.litemax.ECoPro.dto.product;

import com.litemax.ECoPro.dto.auth.UserResponse;
import com.litemax.ECoPro.entity.product.Product;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ProductResponse {
    private Long id;
    private String name;
    private String slug;
    private String shortDescription;
    private String description;
    private String sku;
    private BigDecimal price;
    private BigDecimal comparePrice;
    private BigDecimal costPrice;
    private boolean trackInventory;
    private Integer inventoryQuantity;
    private Integer lowStockThreshold;
    private boolean allowBackorders;
    private Product.ProductStatus status;
    private Product.InventoryStatus inventoryStatus;
    private boolean featured;
    private boolean digital;
    private boolean requiresShipping;
    private BigDecimal weight;
    private String dimensions;
    private String metaTitle;
    private String metaDescription;
    private String metaKeywords;
    private String tags;
    private String vendor;
    private String barcode;
    private String hsnCode;
    private BigDecimal taxPercentage;
    private Integer sortOrder;
    private Long viewCount;
    private BigDecimal rating;
    private Integer reviewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;

    // Relationships
    private UserResponse seller;
    private BrandResponse brand;
    private List<CategoryResponse> categories;
    private List<ProductMediaResponse> media;
    private List<ProductVariantResponse> variants;
    private List<ProductAttributeResponse> attributes;

    // Computed fields
    private boolean inStock;
    private boolean lowStock;
    private BigDecimal discountPercentage;
    private String primaryImageUrl;
    private List<String> tagsList;

    // Additional fields for listings
    private String formattedPrice;
    private String formattedComparePrice;
    private boolean hasDiscount;
    private String stockStatus;
}