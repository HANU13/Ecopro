package com.litemax.ECoPro.dto.inventory;

import lombok.Data;

import jakarta.validation.constraints.*;

@Data
public class InventoryUpdateRequest {
    
    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    private Integer quantity;
    
    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
    
    @Size(max = 100, message = "Location must not exceed 100 characters")
    private String location;
    
    @Min(value = 0, message = "Reorder level cannot be negative")
    private Integer reorderLevel;
    
    @Min(value = 1, message = "Max stock level must be at least 1")
    private Integer maxStockLevel;
}