package com.litemax.ECoPro.entity.cart;

import com.litemax.ECoPro.entity.auth.User;
import com.litemax.ECoPro.entity.product.Product;
import com.litemax.ECoPro.entity.product.ProductVariant;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "wishlists", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "product_id", "variant_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Wishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_public")
    private Boolean isPublic = false;

    @Column(name = "priority")
    private Integer priority = 1; // 1 = Low, 2 = Medium, 3 = High

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    public String getDisplayName() {
        StringBuilder name = new StringBuilder(product.getName());
        if (variant != null) {
            name.append(" - ").append(variant.getDisplayName());
        }
        return name.toString();
    }

    public String getImageUrl() {
        if (variant != null && variant.getImageUrl() != null) {
            return variant.getImageUrl();
        }
        return product.getPrimaryImage() != null ? 
               product.getPrimaryImage().getFileUrl() : null;
    }

    public boolean isInStock() {
        if (variant != null) {
            return variant.isInStock();
        }
        return product.isInStock();
    }

    public boolean isAvailable() {
        return product.getStatus() == Product.ProductStatus.ACTIVE && 
               (variant == null || variant.isActive());
    }

    public String getPriorityText() {
        return switch (priority) {
            case 1 -> "Low";
            case 2 -> "Medium";
            case 3 -> "High";
            default -> "Unknown";
        };
    }
}