package com.litemax.ECoPro.dto.product;

import com.litemax.ECoPro.entity.product.Product;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 255, message = "Product name cannot exceed 255 characters")
    private String name;

    @Size(max = 500, message = "Short description cannot exceed 500 characters")
    private String shortDescription;

    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Price format is invalid")
    private BigDecimal price;

    @DecimalMin(value = "0.01", message = "Compare price must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Compare price format is invalid")
    private BigDecimal comparePrice;

    @Digits(integer = 10, fraction = 2, message = "Cost price format is invalid")
    private BigDecimal costPrice;

    @NotBlank(message = "SKU is required")
    @Size(max = 100, message = "SKU cannot exceed 100 characters")
    @Pattern(regexp = "^[A-Z0-9-_]+$", message = "SKU can only contain uppercase letters, numbers, hyphens and underscores")
    private String sku;

    private boolean trackInventory = true;

    @Min(value = 0, message = "Inventory quantity cannot be negative")
    private Integer inventoryQuantity = 0;

    @Min(value = 1, message = "Low stock threshold must be at least 1")
    private Integer lowStockThreshold = 5;

    private boolean allowBackorders = false;

    @NotNull(message = "Product status is required")
    private Product.ProductStatus status = Product.ProductStatus.DRAFT;

    private boolean featured = false;
    private boolean digital = false;
    private boolean requiresShipping = true;

    @DecimalMin(value = "0.01", message = "Weight must be positive")
    @Digits(integer = 10, fraction = 2, message = "Weight format is invalid")
    private BigDecimal weight;

    @Size(max = 100, message = "Dimensions cannot exceed 100 characters")
    private String dimensions;

    @Size(max = 255, message = "Meta title cannot exceed 255 characters")
    private String metaTitle;

    @Size(max = 500, message = "Meta description cannot exceed 500 characters")
    private String metaDescription;

    @Size(max = 1000, message = "Meta keywords cannot exceed 1000 characters")
    private String metaKeywords;

    private String tags;

    @Size(max = 100, message = "Vendor name cannot exceed 100 characters")
    private String vendor;

    @Size(max = 50, message = "Barcode cannot exceed 50 characters")
    private String barcode;

    @Size(max = 20, message = "HSN code cannot exceed 20 characters")
    private String hsnCode;

    @DecimalMin(value = "0.0", message = "Tax percentage cannot be negative")
    @DecimalMax(value = "100.0", message = "Tax percentage cannot exceed 100")
    @Digits(integer = 3, fraction = 2, message = "Tax percentage format is invalid")
    private BigDecimal taxPercentage = BigDecimal.ZERO;

    @Min(value = 0, message = "Sort order cannot be negative")
    private Integer sortOrder = 0;

    private Long brandId;

    @NotEmpty(message = "At least one category must be selected")
    private List<Long> categoryIds;

    private List<ProductAttributeRequest> attributes;
    private List<ProductMediaRequest> media;
    private List<ProductVariantRequest> variants;
}