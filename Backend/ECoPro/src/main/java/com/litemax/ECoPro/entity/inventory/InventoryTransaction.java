package com.litemax.ECoPro.entity.inventory;

import com.litemax.ECoPro.entity.auth.User;
import com.litemax.ECoPro.entity.order.Order;
import com.litemax.ECoPro.entity.product.Product;
import com.litemax.ECoPro.entity.product.ProductVariant;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class InventoryTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 50)
    private String transactionReference;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id")
    private ProductVariant productVariant;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;
    
    @NotNull
    @Column(nullable = false)
    private Integer quantity;
    
    @Column
    private Integer previousQuantity;
    
    @Column
    private Integer newQuantity;
    
    @Size(max = 500)
    private String reason;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order relatedOrder;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User performedBy;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    public enum TransactionType {
        STOCK_IN,           // Adding stock
        STOCK_OUT,          // Removing stock
        RESERVATION,        // Reserving stock for orders
        RESERVATION_RELEASE, // Releasing reserved stock
        ADJUSTMENT,         // Manual stock adjustment
        SALE,              // Stock reduction due to sale
        RETURN,            // Stock increase due to return
        DAMAGE,            // Stock reduction due to damage
        TRANSFER           // Stock transfer between warehouses
    }
}