package com.litemax.ECoPro.dto.inventory;

import com.litemax.ECoPro.entity.inventory.Inventory.InventoryStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InventoryResponse {
    private Long id;
    private Long warehouseId;
    private String warehouseName;
    private Long productId;
    private String productName;
    private Long productVariantId;
    private String productVariantName;
    private Integer quantityOnHand;
    private Integer quantityReserved;
    private Integer quantityAvailable;
    private Integer reorderLevel;
    private Integer maxStockLevel;
    private InventoryStatus status;
    private String location;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastStockUpdateAt;
}