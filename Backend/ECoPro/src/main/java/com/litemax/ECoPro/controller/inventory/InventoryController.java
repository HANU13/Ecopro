package com.litemax.ECoPro.controller.inventory;

import com.litemax.ECoPro.dto.inventory.*;
import com.litemax.ECoPro.entity.inventory.Inventory.InventoryStatus;
import com.litemax.ECoPro.entity.inventory.InventoryTransaction;
import com.litemax.ECoPro.service.inventory.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {
    
    private final InventoryService inventoryService;
    
    // Public/Customer APIs for stock checking
    @GetMapping("/check-availability")
    public ResponseEntity<Boolean> checkAvailability(@RequestParam Long productId,
                                                   @RequestParam(required = false) Long productVariantId,
                                                   @RequestParam @Min(1) Integer quantity) {
        log.info("Checking availability: Product: {}, Variant: {}, Quantity: {}", 
                productId, productVariantId, quantity);
        
        boolean available = inventoryService.checkAvailability(productId, productVariantId, quantity);
        return ResponseEntity.ok(available);
    }
    
    @GetMapping("/stock-level/{productId}")
    public ResponseEntity<Integer> getStockLevel(@PathVariable Long productId,
                                               @RequestParam(required = false) Long productVariantId) {
        log.info("Getting stock level for product: {}, variant: {}", productId, productVariantId);
        
        Integer stockLevel = inventoryService.getTotalAvailableStock(productId, productVariantId);
        return ResponseEntity.ok(stockLevel);
    }
    
    // Admin/Manager APIs
    @GetMapping("/admin/product/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<List<InventoryResponse>> getProductInventory(@PathVariable Long productId,
                                                                     @RequestParam(required = false) Long productVariantId,
                                                                     Authentication authentication) {
        log.info("Admin getting inventory for product: {}, variant: {} by user: {}", 
                productId, productVariantId, authentication.getName());
        
        List<InventoryResponse> inventory = inventoryService.getProductInventory(productId, productVariantId);
        return ResponseEntity.ok(inventory);
    }
    
    @GetMapping("/admin/warehouse/{warehouseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<Page<InventoryResponse>> getWarehouseInventory(@PathVariable Long warehouseId,
                                                                        @RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "20") int size,
                                                                        @RequestParam(defaultValue = "lastStockUpdateAt") String sortBy,
                                                                        @RequestParam(defaultValue = "desc") String sortDir) {
        log.info("Getting warehouse inventory: {}, page: {}, size: {}", warehouseId, page, size);
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<InventoryResponse> inventory = inventoryService.getWarehouseInventory(warehouseId, pageable);
        return ResponseEntity.ok(inventory);
    }
    
    @PostMapping("/admin/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<InventoryResponse> createInventoryRecord(@RequestParam Long warehouseId,
                                                                 @RequestParam Long productId,
                                                                 @RequestParam(required = false) Long productVariantId,
                                                                 @RequestParam(defaultValue = "0") @Min(0) Integer initialQuantity,
                                                                 Authentication authentication) {
        log.info("Creating inventory record: Warehouse: {}, Product: {}, Variant: {}, Quantity: {}", 
                warehouseId, productId, productVariantId, initialQuantity);
        
        Long userId = getUserIdFromAuthentication(authentication);
        InventoryResponse inventory = inventoryService.createInventoryRecord(
            warehouseId, productId, productVariantId, initialQuantity, userId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(inventory);
    }
    
    @PutMapping("/admin/{inventoryId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<InventoryResponse> updateInventory(@PathVariable Long inventoryId,
                                                           @RequestParam Long warehouseId,
                                                           @RequestParam Long productId,
                                                           @RequestParam(required = false) Long productVariantId,
                                                           @Valid @RequestBody InventoryUpdateRequest request,
                                                           Authentication authentication) {
        log.info("Updating inventory: ID: {}, New Quantity: {}", inventoryId, request.getQuantity());
        
        Long userId = getUserIdFromAuthentication(authentication);
        InventoryResponse inventory = inventoryService.updateStock(
            warehouseId, productId, productVariantId, request, userId);
        
        return ResponseEntity.ok(inventory);
    }
    
    @PostMapping("/admin/adjust")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<Void> adjustStock(@Valid @RequestBody StockAdjustmentRequest request,
                                          Authentication authentication) {
        log.info("Processing stock adjustment: Product: {}, Variant: {}, Quantity: {}, Type: {}", 
                request.getProductId(), request.getProductVariantId(), 
                request.getQuantity(), request.getTransactionType());
        
        Long userId = getUserIdFromAuthentication(authentication);
        inventoryService.adjustStock(request, userId);
        
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/admin/transfer")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> transferStock(@RequestParam Long fromWarehouseId,
                                            @RequestParam Long toWarehouseId,
                                            @RequestParam Long productId,
                                            @RequestParam(required = false) Long productVariantId,
                                            @RequestParam @NotNull @Min(1) Integer quantity,
                                            @RequestParam(required = false) String reason,
                                            Authentication authentication) {
        log.info("Transferring stock: From: {}, To: {}, Product: {}, Quantity: {}", 
                fromWarehouseId, toWarehouseId, productId, quantity);
        
        Long userId = getUserIdFromAuthentication(authentication);
        inventoryService.transferStock(fromWarehouseId, toWarehouseId, productId, 
                                     productVariantId, quantity, reason, userId);
        
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/admin/{inventoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteInventoryRecord(@PathVariable Long inventoryId,
                                                     Authentication authentication) {
        log.info("Deleting inventory record: {}", inventoryId);
        
        Long userId = getUserIdFromAuthentication(authentication);
        inventoryService.deleteInventoryRecord(inventoryId, userId);
        
        return ResponseEntity.ok().build();
    }
    
    // Monitoring and reporting APIs
    @GetMapping("/admin/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<List<InventoryResponse>> getLowStockItems() {
        log.info("Getting low stock items");
        
        List<InventoryResponse> lowStockItems = inventoryService.getLowStockItems();
        return ResponseEntity.ok(lowStockItems);
    }
    
    @GetMapping("/admin/out-of-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<List<InventoryResponse>> getOutOfStockItems() {
        log.info("Getting out of stock items");
        
        List<InventoryResponse> outOfStockItems = inventoryService.getOutOfStockItems();
        return ResponseEntity.ok(outOfStockItems);
    }
    
    @GetMapping("/admin/by-status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<List<InventoryResponse>> getInventoryByStatus(@PathVariable InventoryStatus status) {
        log.info("Getting inventory items with status: {}", status);
        
        List<InventoryResponse> items = inventoryService.getInventoryByStatus(status);
        return ResponseEntity.ok(items);
    }
    
    @GetMapping("/admin/transactions/{inventoryId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<Page<InventoryTransaction>> getInventoryTransactions(@PathVariable Long inventoryId,
                                                                             @RequestParam(defaultValue = "0") int page,
                                                                             @RequestParam(defaultValue = "20") int size) {
        log.info("Getting transactions for inventory: {}", inventoryId);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<InventoryTransaction> transactions = inventoryService.getInventoryTransactions(inventoryId, pageable);
        
        return ResponseEntity.ok(transactions);
    }
    
    @GetMapping("/admin/product-transactions/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<List<InventoryTransaction>> getProductTransactions(@PathVariable Long productId,
                                                                          @RequestParam(required = false) Long productVariantId) {
        log.info("Getting transactions for product: {}, variant: {}", productId, productVariantId);
        
        List<InventoryTransaction> transactions = inventoryService.getProductTransactions(productId, productVariantId);
        return ResponseEntity.ok(transactions);
    }
    
    @GetMapping("/admin/analytics/warehouse-stats/{warehouseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<WarehouseStatsResponse> getWarehouseStats(@PathVariable Long warehouseId) {
        log.info("Getting warehouse statistics: {}", warehouseId);
        
        WarehouseStatsResponse stats = new WarehouseStatsResponse();
        stats.setWarehouseId(warehouseId);
        stats.setActiveItems(inventoryService.getWarehouseItemCount(warehouseId, InventoryStatus.ACTIVE));
        stats.setLowStockItems(inventoryService.getWarehouseItemCount(warehouseId, InventoryStatus.LOW_STOCK));
        stats.setOutOfStockItems(inventoryService.getWarehouseItemCount(warehouseId, InventoryStatus.OUT_OF_STOCK));
        stats.setInactiveItems(inventoryService.getWarehouseItemCount(warehouseId, InventoryStatus.INACTIVE));
        
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/admin/transactions-report")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<InventoryTransaction>> getTransactionsReport(@RequestParam LocalDateTime startDate,
                                                                          @RequestParam LocalDateTime endDate) {
        log.info("Getting transactions report between {} and {}", startDate, endDate);
        
        List<InventoryTransaction> transactions = inventoryService.getTransactionsBetweenDates(startDate, endDate);
        return ResponseEntity.ok(transactions);
    }
    
    private Long getUserIdFromAuthentication(Authentication authentication) {
        return Long.parseLong(authentication.getName()); // Adjust based on your implementation
    }
    
    // Helper response class for warehouse statistics
    @lombok.Data
    public static class WarehouseStatsResponse {
        private Long warehouseId;
        private Long activeItems;
        private Long lowStockItems;
        private Long outOfStockItems;
        private Long inactiveItems;
    }
}