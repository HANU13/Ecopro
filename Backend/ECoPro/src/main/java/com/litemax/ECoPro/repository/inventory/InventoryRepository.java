package com.litemax.ECoPro.repository.inventory;

import com.litemax.ECoPro.entity.inventory.Inventory;
import com.litemax.ECoPro.entity.inventory.Inventory.InventoryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    
    Optional<Inventory> findByWarehouseIdAndProductIdAndProductVariantId(
        Long warehouseId, Long productId, Long productVariantId);
    
    Optional<Inventory> findByWarehouseIdAndProductIdAndProductVariantIsNull(
        Long warehouseId, Long productId);
    
    List<Inventory> findByProductId(Long productId);
    
    List<Inventory> findByProductIdAndProductVariantId(Long productId, Long productVariantId);
    
    List<Inventory> findByWarehouseId(Long warehouseId);
    
    List<Inventory> findByStatus(InventoryStatus status);
    
    Page<Inventory> findByWarehouseId(Long warehouseId, Pageable pageable);
    
    @Query("SELECT i FROM Inventory i WHERE i.quantityOnHand <= i.reorderLevel AND i.status = 'ACTIVE'")
    List<Inventory> findLowStockItems();
    
    @Query("SELECT i FROM Inventory i WHERE i.status = 'OUT_OF_STOCK'")
    List<Inventory> findOutOfStockItems();
    
    @Query("SELECT SUM(i.quantityOnHand) FROM Inventory i WHERE i.product.id = :productId")
    Integer getTotalStockForProduct(@Param("productId") Long productId);
    
    @Query("SELECT SUM(i.quantityAvailable) FROM Inventory i WHERE i.product.id = :productId")
    Integer getTotalAvailableStockForProduct(@Param("productId") Long productId);
    
    @Query("SELECT SUM(i.quantityOnHand) FROM Inventory i WHERE i.product.id = :productId AND i.productVariant.id = :variantId")
    Integer getTotalStockForProductVariant(@Param("productId") Long productId, @Param("variantId") Long variantId);
    
    @Query("SELECT SUM(i.quantityAvailable) FROM Inventory i WHERE i.product.id = :productId AND i.productVariant.id = :variantId")
    Integer getTotalAvailableStockForProductVariant(@Param("productId") Long productId, @Param("variantId") Long variantId);
    
    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.warehouse.id = :warehouseId AND i.status = :status")
    Long countByWarehouseIdAndStatus(@Param("warehouseId") Long warehouseId, @Param("status") InventoryStatus status);
}