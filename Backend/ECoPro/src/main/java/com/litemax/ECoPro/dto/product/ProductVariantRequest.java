package com.litemax.ECoPro.dto.product;

import com.litemax.ECoPro.entity.product.ProductVariant;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductVariantRequest {

    @NotBlank(message = "Variant name is required")
    private String name;

    @NotBlank(message = "SKU is required")
    @Pattern(regexp = "^[A-Z0-9-_]+$", message = "SKU can only contain uppercase letters, numbers, hyphens and underscores")
    private String sku;

    private String option1Name;
    private String option1Value;
    private String option2Name;
    private String option2Value;
    private String option3Name;
    private String option3Value;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Price format is invalid")
    private BigDecimal price;

    @Digits(integer = 10, fraction = 2, message = "Compare price format is invalid")
    private BigDecimal comparePrice;

    @Digits(integer = 10, fraction = 2, message = "Cost price format is invalid")
    private BigDecimal costPrice;

    @Min(value = 0, message = "Inventory quantity cannot be negative")
    private Integer inventoryQuantory = 0;

    private ProductVariant.InventoryPolicy inventoryPolicy = ProductVariant.InventoryPolicy.DENY;

    @Digits(integer = 10, fraction = 2, message = "Weight format is invalid")
    private BigDecimal weight;

    private String barcode;
    private String imageUrl;
    private boolean active = true;
    private Integer sortOrder = 0;
}