package com.litemax.ECoPro.service.inventory;

import com.litemax.ECoPro.dto.inventory.*;
import com.litemax.ECoPro.entity.auth.User;
import com.litemax.ECoPro.entity.inventory.Inventory;
import com.litemax.ECoPro.entity.inventory.Inventory.InventoryStatus;
import com.litemax.ECoPro.entity.inventory.InventoryTransaction;
import com.litemax.ECoPro.entity.inventory.InventoryTransaction.TransactionType;
import com.litemax.ECoPro.entity.inventory.Warehouse;
import com.litemax.ECoPro.entity.product.Product;
import com.litemax.ECoPro.entity.product.ProductVariant;
import com.litemax.ECoPro.exception.ResourceNotFoundException;
import com.litemax.ECoPro.exception.ValidationException;
import com.litemax.ECoPro.repository.inventory.InventoryRepository;
import com.litemax.ECoPro.repository.inventory.InventoryTransactionRepository;
import com.litemax.ECoPro.repository.inventory.WarehouseRepository;
import com.litemax.ECoPro.repository.product.ProductRepository;
import com.litemax.ECoPro.repository.product.ProductVariantRepository;
import com.litemax.ECoPro.service.UserService;
import com.litemax.ECoPro.util.MapperUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {
    
    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserService userService;
    private final MapperUtil mapperUtil;
    
    @Transactional(readOnly = true)
    public boolean checkAvailability(Long productId, Long productVariantId, Integer requiredQuantity) {
        log.info("Checking availability: Product ID: {}, Variant ID: {}, Required Quantity: {}", 
                productId, productVariantId, requiredQuantity);
        
        if (requiredQuantity == null || requiredQuantity <= 0) {
            log.error("Invalid quantity requested: {}", requiredQuantity);
            return false;
        }
        
        Integer availableStock = getTotalAvailableStock(productId, productVariantId);
        boolean isAvailable = availableStock >= requiredQuantity;
        
        log.debug("Availability check result: Product: {}, Variant: {}, Available: {}, Required: {}, Available: {}", 
                productId, productVariantId, availableStock, requiredQuantity, isAvailable);
        
        return isAvailable;
    }
    
    @Transactional
    public void reserveInventory(Long productId, Long productVariantId, Integer quantity) {
        log.info("Reserving inventory: Product ID: {}, Variant ID: {}, Quantity: {}", 
                productId, productVariantId, quantity);
        
        if (quantity == null || quantity <= 0) {
            log.error("Invalid quantity for reservation: {}", quantity);
            throw new ValidationException("Invalid quantity for reservation");
        }
        
        // Check availability first
        if (!checkAvailability(productId, productVariantId, quantity)) {
            log.error("Insufficient stock for reservation: Product: {}, Variant: {}, Required: {}", 
                     productId, productVariantId, quantity);
            throw new ValidationException("Insufficient stock for reservation");
        }
        
        // Get inventory records for this product/variant
        List<Inventory> inventoryList = getInventoryRecords(productId, productVariantId);
        
        if (inventoryList.isEmpty()) {
            log.error("No inventory found for product: {} variant: {}", productId, productVariantId);
            throw new ResourceNotFoundException("No inventory found for the product");
        }
        
        // Reserve inventory across warehouses (FIFO - First warehouse first)
        int remainingToReserve = quantity;
        for (Inventory inventory : inventoryList) {
            if (remainingToReserve <= 0) break;
            
            int availableInWarehouse = inventory.getQuantityAvailable();
            if (availableInWarehouse > 0) {
                int toReserveFromWarehouse = Math.min(remainingToReserve, availableInWarehouse);
                
                // Update inventory
                inventory.setQuantityReserved(inventory.getQuantityReserved() + toReserveFromWarehouse);
                inventoryRepository.save(inventory);
                
                // Create transaction record
                createInventoryTransaction(inventory, TransactionType.RESERVATION, toReserveFromWarehouse, 
                                         "Stock reserved for order", null, null);
                
                remainingToReserve -= toReserveFromWarehouse;
                
                log.debug("Reserved {} units from warehouse {} for product {}", 
                         toReserveFromWarehouse, inventory.getWarehouse().getId(), productId);
            }
        }
        
        if (remainingToReserve > 0) {
            log.error("Could not reserve full quantity: Product: {}, Remaining: {}", productId, remainingToReserve);
            throw new ValidationException("Could not reserve the full requested quantity");
        }
        
        log.info("Successfully reserved {} units for product: {} variant: {}", quantity, productId, productVariantId);
    }
    
    @Transactional
    public void releaseInventory(Long productId, Long productVariantId, Integer quantity) {
        log.info("Releasing inventory: Product ID: {}, Variant ID: {}, Quantity: {}", 
                productId, productVariantId, quantity);
        
        if (quantity == null || quantity <= 0) {
            log.error("Invalid quantity for release: {}", quantity);
            throw new ValidationException("Invalid quantity for release");
        }
        
        // Get inventory records for this product/variant
        List<Inventory> inventoryList = getInventoryRecords(productId, productVariantId);
        
        if (inventoryList.isEmpty()) {
            log.warn("No inventory found for product: {} variant: {}", productId, productVariantId);
            return; // Nothing to release
        }
        
        // Release inventory across warehouses (FIFO)
        int remainingToRelease = quantity;
        for (Inventory inventory : inventoryList) {
            if (remainingToRelease <= 0) break;
            
            int reservedInWarehouse = inventory.getQuantityReserved();
            if (reservedInWarehouse > 0) {
                int toReleaseFromWarehouse = Math.min(remainingToRelease, reservedInWarehouse);
                
                // Update inventory
                inventory.setQuantityReserved(inventory.getQuantityReserved() - toReleaseFromWarehouse);
                inventoryRepository.save(inventory);
                
                // Create transaction record
                createInventoryTransaction(inventory, TransactionType.RESERVATION_RELEASE, toReleaseFromWarehouse, 
                                         "Stock reservation released", null, null);
                
                remainingToRelease -= toReleaseFromWarehouse;
                
                log.debug("Released {} reserved units from warehouse {} for product {}", 
                         toReleaseFromWarehouse, inventory.getWarehouse().getId(), productId);
            }
        }
        
        log.info("Successfully released {} units for product: {} variant: {}", 
                quantity - remainingToRelease, productId, productVariantId);
    }
    
    @Transactional
    public void commitInventoryReduction(Long productId, Long productVariantId, Integer quantity, String reason) {
        log.info("Committing inventory reduction: Product ID: {}, Variant ID: {}, Quantity: {}", 
                productId, productVariantId, quantity);
        
        if (quantity == null || quantity <= 0) {
            log.error("Invalid quantity for reduction: {}", quantity);
            throw new ValidationException("Invalid quantity for reduction");
        }
        
        // Get inventory records for this product/variant
        List<Inventory> inventoryList = getInventoryRecords(productId, productVariantId);
        
        if (inventoryList.isEmpty()) {
            log.error("No inventory found for product: {} variant: {}", productId, productVariantId);
            throw new ResourceNotFoundException("No inventory found for the product");
        }
        
        // Reduce inventory across warehouses (FIFO)
        int remainingToReduce = quantity;
        for (Inventory inventory : inventoryList) {
            if (remainingToReduce <= 0) break;
            
            int onHandInWarehouse = inventory.getQuantityOnHand();
            if (onHandInWarehouse > 0) {
                int toReduceFromWarehouse = Math.min(remainingToReduce, onHandInWarehouse);
                
                // Update inventory - reduce both reserved and on-hand quantities
                int reservedToReduce = Math.min(toReduceFromWarehouse, inventory.getQuantityReserved());
                
                inventory.setQuantityOnHand(inventory.getQuantityOnHand() - toReduceFromWarehouse);
                inventory.setQuantityReserved(inventory.getQuantityReserved() - reservedToReduce);
                inventoryRepository.save(inventory);
                
                // Create transaction record
                createInventoryTransaction(inventory, TransactionType.SALE, toReduceFromWarehouse, 
                                         reason != null ? reason : "Stock sold", null, null);
                
                remainingToReduce -= toReduceFromWarehouse;
                
                log.debug("Reduced {} units from warehouse {} for product {}", 
                         toReduceFromWarehouse, inventory.getWarehouse().getId(), productId);
            }
        }
        
        if (remainingToReduce > 0) {
            log.warn("Could not reduce full quantity: Product: {}, Remaining: {}", productId, remainingToReduce);
        }
        
        log.info("Successfully reduced {} units for product: {} variant: {}", 
                quantity - remainingToReduce, productId, productVariantId);
    }
    
    @Transactional
    public InventoryResponse updateStock(Long warehouseId, Long productId, Long productVariantId, 
                                       InventoryUpdateRequest request, Long userId) {
        log.info("Updating stock: Warehouse: {}, Product: {}, Variant: {}, New Quantity: {}", 
                warehouseId, productId, productVariantId, request.getQuantity());
        
        // Validate entities exist
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
            .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        ProductVariant variant = null;
        if (productVariantId != null) {
            variant = productVariantRepository.findById(productVariantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found"));
        }
        
        // Find or create inventory record
        Optional<Inventory> existingInventory;
        if (productVariantId != null) {
            existingInventory = inventoryRepository.findByWarehouseIdAndProductIdAndProductVariantId(
                warehouseId, productId, productVariantId);
        } else {
            existingInventory = inventoryRepository.findByWarehouseIdAndProductIdAndProductVariantIsNull(
                warehouseId, productId);
        }
        
        Inventory inventory;
        boolean isNew = false;
        
        if (existingInventory.isPresent()) {
            inventory = existingInventory.get();
        } else {
            // Create new inventory record
            inventory = new Inventory();
            inventory.setWarehouse(warehouse);
            inventory.setProduct(product);
            inventory.setProductVariant(variant);
            inventory.setQuantityOnHand(0);
            inventory.setQuantityReserved(0);
            inventory.setStatus(InventoryStatus.ACTIVE);
            isNew = true;
        }
        
        // Calculate adjustment
        Integer previousQuantity = inventory.getQuantityOnHand();
        Integer adjustment = request.getQuantity() - previousQuantity;
        
        // Update inventory
        inventory.setQuantityOnHand(request.getQuantity());
        if (request.getReorderLevel() != null) {
            inventory.setReorderLevel(request.getReorderLevel());
        }
        if (request.getMaxStockLevel() != null) {
            inventory.setMaxStockLevel(request.getMaxStockLevel());
        }
        if (request.getLocation() != null) {
            inventory.setLocation(request.getLocation());
        }
        
        inventory = inventoryRepository.save(inventory);
        
        // Create transaction record for the adjustment
        if (adjustment != 0) {
            TransactionType transactionType = adjustment > 0 ? TransactionType.STOCK_IN : TransactionType.STOCK_OUT;
            createInventoryTransaction(inventory, transactionType, Math.abs(adjustment), 
                                     request.getReason() != null ? request.getReason() : "Manual stock adjustment", 
                                     null, userId);
        }
        
        log.info("Stock updated successfully: {} -> {} (adjustment: {})", 
                previousQuantity, request.getQuantity(), adjustment);
        
        return mapperUtil.mapToInventoryResponse(inventory);
    }
    
    @Transactional
    public void adjustStock(StockAdjustmentRequest request, Long userId) {
        log.info("Processing stock adjustment: Product: {}, Variant: {}, Quantity: {}, Type: {}", 
                request.getProductId(), request.getProductVariantId(), 
                request.getQuantity(), request.getTransactionType());
        
        // Find inventory record
        Optional<Inventory> inventoryOpt;
        if (request.getProductVariantId() != null) {
            inventoryOpt = inventoryRepository.findByWarehouseIdAndProductIdAndProductVariantId(
                request.getWarehouseId(), request.getProductId(), request.getProductVariantId());
        } else {
            inventoryOpt = inventoryRepository.findByWarehouseIdAndProductIdAndProductVariantIsNull(
                request.getWarehouseId(), request.getProductId());
        }
        
        if (!inventoryOpt.isPresent()) {
            log.error("Inventory record not found for adjustment: Warehouse: {}, Product: {}, Variant: {}", 
                     request.getWarehouseId(), request.getProductId(), request.getProductVariantId());
            throw new ResourceNotFoundException("Inventory record not found");
        }
        
        Inventory inventory = inventoryOpt.get();
        Integer previousQuantity = inventory.getQuantityOnHand();
        
        // Apply adjustment based on transaction type
        switch (request.getTransactionType()) {
            case STOCK_IN:
            case RETURN:
                inventory.setQuantityOnHand(inventory.getQuantityOnHand() + request.getQuantity());
                break;
            case STOCK_OUT:
            case DAMAGE:
            case SALE:
                if (inventory.getQuantityOnHand() < request.getQuantity()) {
                    log.error("Insufficient stock for adjustment: Available: {}, Required: {}", 
                             inventory.getQuantityOnHand(), request.getQuantity());
                    throw new ValidationException("Insufficient stock for adjustment");
                }
                inventory.setQuantityOnHand(inventory.getQuantityOnHand() - request.getQuantity());
                break;
            case ADJUSTMENT:
                // For adjustments, the quantity represents the new total quantity
                inventory.setQuantityOnHand(request.getQuantity());
                break;
            default:
                log.error("Unsupported transaction type for stock adjustment: {}", request.getTransactionType());
                throw new ValidationException("Unsupported transaction type");
        }
        
        inventory = inventoryRepository.save(inventory);
        
        // Create transaction record
        createInventoryTransaction(inventory, request.getTransactionType(), 
                                 Math.abs(request.getQuantity()), request.getReason(), null, userId);
        
        log.info("Stock adjustment completed: {} -> {} for product: {}", 
                previousQuantity, inventory.getQuantityOnHand(), request.getProductId());
    }

    @Transactional(readOnly = true)
    public Integer getTotalAvailableStock(Long productId, Long productVariantId) {
        log.debug("Getting total available stock: Product: {}, Variant: {}", productId, productVariantId);

        Integer totalStock;
        if (productVariantId != null) {
            totalStock = inventoryRepository.getTotalAvailableStockForProductVariant(productId, productVariantId);
        } else {
            totalStock = inventoryRepository.getTotalAvailableStockForProduct(productId);
        }

        return totalStock != null ? totalStock : 0;
    }

    @Transactional(readOnly = true)
    public Integer getTotalStock(Long productId, Long productVariantId) {
        log.debug("Getting total stock: Product: {}, Variant: {}", productId, productVariantId);

        Integer totalStock;
        if (productVariantId != null) {
            totalStock = inventoryRepository.getTotalStockForProductVariant(productId, productVariantId);
        } else {
            totalStock = inventoryRepository.getTotalStockForProduct(productId);
        }

        return totalStock != null ? totalStock : 0;
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getProductInventory(Long productId, Long productVariantId) {
        log.info("Getting inventory for product: {}, variant: {}", productId, productVariantId);

        List<Inventory> inventoryList;
        if (productVariantId != null) {
            inventoryList = inventoryRepository.findByProductIdAndProductVariantId(productId, productVariantId);
        } else {
            inventoryList = inventoryRepository.findByProductId(productId);
        }

        log.debug("Found {} inventory records for product: {}", inventoryList.size(), productId);

        return inventoryList.stream()
                .map(mapperUtil::mapToInventoryResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<InventoryResponse> getWarehouseInventory(Long warehouseId, Pageable pageable) {
        log.info("Getting inventory for warehouse: {}", warehouseId);

        Page<Inventory> inventoryPage = inventoryRepository.findByWarehouseId(warehouseId, pageable);
        log.debug("Found {} inventory records for warehouse: {}", inventoryPage.getTotalElements(), warehouseId);

        return inventoryPage.map(mapperUtil::mapToInventoryResponse);
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getLowStockItems() {
        log.info("Getting low stock items");

        List<Inventory> lowStockItems = inventoryRepository.findLowStockItems();
        log.debug("Found {} low stock items", lowStockItems.size());

        return lowStockItems.stream()
                .map(mapperUtil::mapToInventoryResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getOutOfStockItems() {
        log.info("Getting out of stock items");

        List<Inventory> outOfStockItems = inventoryRepository.findOutOfStockItems();
        log.debug("Found {} out of stock items", outOfStockItems.size());

        return outOfStockItems.stream()
                .map(mapperUtil::mapToInventoryResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getInventoryByStatus(InventoryStatus status) {
        log.info("Getting inventory items with status: {}", status);

        List<Inventory> inventoryItems = inventoryRepository.findByStatus(status);
        log.debug("Found {} inventory items with status: {}", inventoryItems.size(), status);

        return inventoryItems.stream()
                .map(mapperUtil::mapToInventoryResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<InventoryTransaction> getInventoryTransactions(Long inventoryId, Pageable pageable) {
        log.info("Getting inventory transactions for inventory: {}", inventoryId);

        Page<InventoryTransaction> transactions = transactionRepository.findByInventoryId(inventoryId, pageable);
        log.debug("Found {} transactions for inventory: {}", transactions.getTotalElements(), inventoryId);

        return transactions;
    }

    @Transactional(readOnly = true)
    public List<InventoryTransaction> getProductTransactions(Long productId, Long productVariantId) {
        log.info("Getting transactions for product: {}, variant: {}", productId, productVariantId);

        List<InventoryTransaction> transactions;
        if (productVariantId != null) {
            transactions = transactionRepository.findByProductIdAndProductVariantId(productId, productVariantId);
        } else {
            transactions = transactionRepository.findByProductId(productId);
        }

        log.debug("Found {} transactions for product: {}", transactions.size(), productId);
        return transactions;
    }

    @Transactional
    public InventoryResponse createInventoryRecord(Long warehouseId, Long productId, Long productVariantId,
                                                   Integer initialQuantity, Long userId) {
        log.info("Creating inventory record: Warehouse: {}, Product: {}, Variant: {}, Quantity: {}",
                warehouseId, productId, productVariantId, initialQuantity);

        // Validate entities exist
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        ProductVariant variant = null;
        if (productVariantId != null) {
            variant = productVariantRepository.findById(productVariantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product variant not found"));
        }

        // Check if inventory record already exists
        Optional<Inventory> existingInventory;
        if (productVariantId != null) {
            existingInventory = inventoryRepository.findByWarehouseIdAndProductIdAndProductVariantId(
                    warehouseId, productId, productVariantId);
        } else {
            existingInventory = inventoryRepository.findByWarehouseIdAndProductIdAndProductVariantIsNull(
                    warehouseId, productId);
        }

        if (existingInventory.isPresent()) {
            log.error("Inventory record already exists for warehouse: {}, product: {}, variant: {}",
                    warehouseId, productId, productVariantId);
            throw new ValidationException("Inventory record already exists for this product in the warehouse");
        }

        // Create new inventory record
        Inventory inventory = new Inventory();
        inventory.setWarehouse(warehouse);
        inventory.setProduct(product);
        inventory.setProductVariant(variant);
        inventory.setQuantityOnHand(initialQuantity != null ? initialQuantity : 0);
        inventory.setQuantityReserved(0);
        inventory.setReorderLevel(10); // Default reorder level
        inventory.setMaxStockLevel(1000); // Default max stock level
        inventory.setStatus(InventoryStatus.ACTIVE);

        inventory = inventoryRepository.save(inventory);

        // Create initial transaction if quantity > 0
        if (initialQuantity != null && initialQuantity > 0) {
            createInventoryTransaction(inventory, TransactionType.STOCK_IN, initialQuantity,
                    "Initial stock entry", null, userId);
        }

        log.info("Inventory record created successfully with ID: {}", inventory.getId());
        return mapperUtil.mapToInventoryResponse(inventory);
    }

    @Transactional
    public void deleteInventoryRecord(Long inventoryId, Long userId) {
        log.info("Deleting inventory record: {}", inventoryId);

        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found"));

        // Check if inventory has stock or reservations
        if (inventory.getQuantityOnHand() > 0 || inventory.getQuantityReserved() > 0) {
            log.error("Cannot delete inventory record with existing stock or reservations: {}", inventoryId);
            throw new ValidationException("Cannot delete inventory record with existing stock or reservations");
        }

        // Create transaction record for deletion
        createInventoryTransaction(inventory, TransactionType.ADJUSTMENT, 0,
                "Inventory record deleted", null, userId);

        inventoryRepository.delete(inventory);
        log.info("Inventory record deleted successfully: {}", inventoryId);
    }

    @Transactional
    public void transferStock(Long fromWarehouseId, Long toWarehouseId, Long productId,
                              Long productVariantId, Integer quantity, String reason, Long userId) {
        log.info("Transferring stock: From: {}, To: {}, Product: {}, Variant: {}, Quantity: {}",
                fromWarehouseId, toWarehouseId, productId, productVariantId, quantity);

        if (quantity <= 0) {
            throw new ValidationException("Transfer quantity must be greater than 0");
        }

        // Find source inventory
        Optional<Inventory> sourceInventoryOpt;
        if (productVariantId != null) {
            sourceInventoryOpt = inventoryRepository.findByWarehouseIdAndProductIdAndProductVariantId(
                    fromWarehouseId, productId, productVariantId);
        } else {
            sourceInventoryOpt = inventoryRepository.findByWarehouseIdAndProductIdAndProductVariantIsNull(
                    fromWarehouseId, productId);
        }

        if (!sourceInventoryOpt.isPresent()) {
            throw new ResourceNotFoundException("Source inventory not found");
        }

        Inventory sourceInventory = sourceInventoryOpt.get();

        // Check availability
        if (sourceInventory.getQuantityAvailable() < quantity) {
            throw new ValidationException("Insufficient available stock for transfer");
        }

        // Find or create destination inventory
        Optional<Inventory> destInventoryOpt;
        if (productVariantId != null) {
            destInventoryOpt = inventoryRepository.findByWarehouseIdAndProductIdAndProductVariantId(
                    toWarehouseId, productId, productVariantId);
        } else {
            destInventoryOpt = inventoryRepository.findByWarehouseIdAndProductIdAndProductVariantIsNull(
                    toWarehouseId, productId);
        }

        Inventory destInventory;
        if (destInventoryOpt.isPresent()) {
            destInventory = destInventoryOpt.get();
        } else {
            // Create destination inventory record
            Warehouse toWarehouse = warehouseRepository.findById(toWarehouseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Destination warehouse not found"));

            destInventory = new Inventory();
            destInventory.setWarehouse(toWarehouse);
            destInventory.setProduct(sourceInventory.getProduct());
            destInventory.setProductVariant(sourceInventory.getProductVariant());
            destInventory.setQuantityOnHand(0);
            destInventory.setQuantityReserved(0);
            destInventory.setReorderLevel(sourceInventory.getReorderLevel());
            destInventory.setMaxStockLevel(sourceInventory.getMaxStockLevel());
            destInventory.setStatus(InventoryStatus.ACTIVE);
        }

        // Perform transfer
        sourceInventory.setQuantityOnHand(sourceInventory.getQuantityOnHand() - quantity);
        destInventory.setQuantityOnHand(destInventory.getQuantityOnHand() + quantity);

        inventoryRepository.save(sourceInventory);
        inventoryRepository.save(destInventory);

        // Create transaction records
        String transferReason = reason != null ? reason : "Stock transfer between warehouses";
        createInventoryTransaction(sourceInventory, TransactionType.TRANSFER, quantity,
                transferReason + " (OUT to warehouse " + toWarehouseId + ")", null, userId);
        createInventoryTransaction(destInventory, TransactionType.TRANSFER, quantity,
                transferReason + " (IN from warehouse " + fromWarehouseId + ")", null, userId);

        log.info("Stock transfer completed successfully: {} units from warehouse {} to {}",
                quantity, fromWarehouseId, toWarehouseId);
    }

    // Analytics and reporting methods
    @Transactional(readOnly = true)
    public Long getWarehouseItemCount(Long warehouseId, InventoryStatus status) {
        log.debug("Getting item count for warehouse: {} with status: {}", warehouseId, status);
        return inventoryRepository.countByWarehouseIdAndStatus(warehouseId, status);
    }

    @Transactional(readOnly = true)
    public List<InventoryTransaction> getTransactionsBetweenDates(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Getting transactions between {} and {}", startDate, endDate);

        List<InventoryTransaction> transactions = transactionRepository.findTransactionsBetweenDates(startDate, endDate);
        log.debug("Found {} transactions between specified dates", transactions.size());

        return transactions;
    }

    // Private helper methods
    private List<Inventory> getInventoryRecords(Long productId, Long productVariantId) {
        if (productVariantId != null) {
            return inventoryRepository.findByProductIdAndProductVariantId(productId, productVariantId);
        } else {
            return inventoryRepository.findByProductId(productId);
        }
    }

    private void createInventoryTransaction(Inventory inventory, TransactionType transactionType,
                                            Integer quantity, String reason, Long orderId, Long userId) {
        try {
            InventoryTransaction transaction = new InventoryTransaction();
            transaction.setTransactionReference(generateTransactionReference());
            transaction.setInventory(inventory);
            transaction.setProduct(inventory.getProduct());
            transaction.setProductVariant(inventory.getProductVariant());
            transaction.setTransactionType(transactionType);
            transaction.setQuantity(quantity);
            transaction.setPreviousQuantity(inventory.getQuantityOnHand());
            transaction.setReason(reason);

            if (orderId != null) {
                // Set order if provided - you might need to load the order entity
                // transaction.setRelatedOrder(orderRepository.findById(orderId).orElse(null));
            }

            if (userId != null) {
                User user = userService.findById(userId);
                transaction.setPerformedBy(user);
            }

            transactionRepository.save(transaction);

            log.debug("Inventory transaction created: {} - {} units - {}",
                    transactionType, quantity, reason);

        } catch (Exception e) {
            log.error("Error creating inventory transaction: {}", e.getMessage(), e);
            // Don't throw here to avoid breaking the main transaction
        }
    }

    private String generateTransactionReference() {
        return "TXN-" + System.currentTimeMillis() + "-" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
