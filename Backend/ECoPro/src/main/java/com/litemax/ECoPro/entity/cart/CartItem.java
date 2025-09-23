package com.litemax.ECoPro.entity.cart;

import com.litemax.ECoPro.entity.product.Product;
import com.litemax.ECoPro.entity.product.ProductVariant;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cart_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "compare_price", precision = 10, scale = 2)
    private BigDecimal comparePrice;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "custom_attributes", columnDefinition = "TEXT")
    private String customAttributes; // JSON string for additional attributes

    @Column(name = "gift_wrap")
    private Boolean giftWrap = false;

    @Column(name = "gift_message", columnDefinition = "TEXT")
    private String giftMessage;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    public void updateTotalPrice() {
        this.totalPrice = this.unitPrice.multiply(new BigDecimal(this.quantity));
    }

    public BigDecimal getSavingsAmount() {
        if (comparePrice == null || comparePrice.compareTo(unitPrice) <= 0) {
            return BigDecimal.ZERO;
        }
        return comparePrice.subtract(unitPrice).multiply(new BigDecimal(quantity));
    }

    public String getDisplayName() {
        StringBuilder name = new StringBuilder(product.getName());
        if (variant != null) {
            name.append(" - ").append(variant.getDisplayName());
        }
        return name.toString();
    }

    public boolean isInStock() {
        if (variant != null) {
            return variant.isInStock() || 
                   variant.getInventoryPolicy() == ProductVariant.InventoryPolicy.CONTINUE;
        }
        return product.isInStock() || product.isAllowBackorders();
    }

    public boolean hasEnoughStock(int requestedQuantity) {
        if (variant != null) {
            return variant.getInventoryQuantory() >= requestedQuantity ||
                   variant.getInventoryPolicy() == ProductVariant.InventoryPolicy.CONTINUE;
        }
        return product.getInventoryQuantity() >= requestedQuantity || 
               product.isAllowBackorders();
    }

    public String getImageUrl() {
        if (variant != null && variant.getImageUrl() != null) {
            return variant.getImageUrl();
        }
        return product.getPrimaryImage() != null ? 
               product.getPrimaryImage().getFileUrl() : null;
    }
}