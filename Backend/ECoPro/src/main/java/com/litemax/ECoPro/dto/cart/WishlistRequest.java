package com.litemax.ECoPro.dto.cart;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class WishlistRequest {
    
    @NotNull(message = "Product ID is required")
    private Long productId;
    
    private Long variantId;
    private String notes;
    private Boolean isPublic = false;
    
    @Min(value = 1, message = "Priority must be between 1 and 3")
    @Max(value = 3, message = "Priority must be between 1 and 3")
    private Integer priority = 1;
}