package com.litemax.ECoPro.dto.inventory;

import com.litemax.ECoPro.entity.inventory.InventoryTransaction.TransactionType;
import lombok.Data;

import jakarta.validation.constraints.*;

@Data
public class StockAdjustmentRequest {
    
    @NotNull(message = "Product ID is required")
    private Long productId;
    
    private Long productVariantId;
    
    @NotNull(message = "Warehouse ID is required")
    private Long warehouseId;
    
    @NotNull(message = "Quantity is required")
    private Integer quantity;
    
    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;
    
    @NotBlank(message = "Reason is required")
    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}