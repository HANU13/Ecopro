package com.litemax.ECoPro.entity.inventory;

import com.litemax.ECoPro.entity.product.Product;
import com.litemax.ECoPro.entity.product.ProductVariant;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"warehouse_id", "product_id", "product_variant_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Inventory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id")
    private ProductVariant productVariant;
    
    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Integer quantityOnHand = 0;
    
    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Integer quantityReserved = 0;
    
    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Integer quantityAvailable = 0;
    
    @Min(0)
    @Column
    private Integer reorderLevel = 10;
    
    @Min(0)
    @Column
    private Integer maxStockLevel = 1000;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InventoryStatus status = InventoryStatus.ACTIVE;
    
    @Size(max = 100)
    private String location; // Shelf/Bin location in warehouse
    
    @Size(max = 500)
    private String notes;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @Column
    private LocalDateTime lastStockUpdateAt;
    
    public enum InventoryStatus {
        ACTIVE, INACTIVE, DISCONTINUED, OUT_OF_STOCK, LOW_STOCK
    }
    
    // Helper method to calculate available quantity
    @PrePersist
    @PreUpdate
    private void calculateAvailableQuantity() {
        this.quantityAvailable = this.quantityOnHand - this.quantityReserved;
        
        // Update status based on stock levels
        if (this.quantityOnHand == 0) {
            this.status = InventoryStatus.OUT_OF_STOCK;
        } else if (this.quantityOnHand <= this.reorderLevel) {
            this.status = InventoryStatus.LOW_STOCK;
        } else if (this.status == InventoryStatus.OUT_OF_STOCK || this.status == InventoryStatus.LOW_STOCK) {
            this.status = InventoryStatus.ACTIVE;
        }
        
        this.lastStockUpdateAt = LocalDateTime.now();
    }
}