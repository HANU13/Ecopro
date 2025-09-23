package com.litemax.ECoPro.repository.inventory;

import com.litemax.ECoPro.entity.inventory.InventoryTransaction;
import com.litemax.ECoPro.entity.inventory.InventoryTransaction.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
    
    List<InventoryTransaction> findByInventoryId(Long inventoryId);
    
    List<InventoryTransaction> findByProductId(Long productId);
    
    List<InventoryTransaction> findByProductIdAndProductVariantId(Long productId, Long productVariantId);
    
    List<InventoryTransaction> findByTransactionType(TransactionType transactionType);
    
    Page<InventoryTransaction> findByInventoryId(Long inventoryId, Pageable pageable);
    
    @Query("SELECT it FROM InventoryTransaction it WHERE it.createdAt BETWEEN :startDate AND :endDate")
    List<InventoryTransaction> findTransactionsBetweenDates(
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT it FROM InventoryTransaction it WHERE it.inventory.warehouse.id = :warehouseId")
    Page<InventoryTransaction> findByWarehouseId(@Param("warehouseId") Long warehouseId, Pageable pageable);
}
