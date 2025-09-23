package com.litemax.ECoPro.entity.product;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product_variants")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(name = "option1_name")
    private String option1Name; // e.g., "Size"

    @Column(name = "option1_value")
    private String option1Value; // e.g., "Large"

    @Column(name = "option2_name")
    private String option2Name; // e.g., "Color"

    @Column(name = "option2_value")
    private String option2Value; // e.g., "Red"

    @Column(name = "option3_name")
    private String option3Name; // e.g., "Material"

    @Column(name = "option3_value")
    private String option3Value; // e.g., "Cotton"

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "compare_price", precision = 10, scale = 2)
    private BigDecimal comparePrice;

    @Column(name = "cost_price", precision = 10, scale = 2)
    private BigDecimal costPrice;

    @Column(name = "inventory_quantity")
    private Integer inventoryQuantory = 0;

    @Column(name = "inventory_policy")
    @Enumerated(EnumType.STRING)
    private InventoryPolicy inventoryPolicy = InventoryPolicy.DENY;

    @Column(name = "weight")
    private BigDecimal weight;

    @Column(name = "barcode")
    private String barcode;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "is_active")
    private boolean active = true;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VariantAttributeValue> attributeValues = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum InventoryPolicy {
        DENY, // Don't sell when out of stock
        CONTINUE // Allow selling when out of stock
    }

    // Helper Methods
    public boolean isInStock() {
        return inventoryQuantory != null && inventoryQuantory > 0;
    }

    public String getDisplayName() {
        StringBuilder displayName = new StringBuilder();
        
        if (option1Value != null) {
            displayName.append(option1Value);
        }
        if (option2Value != null) {
            if (displayName.length() > 0) displayName.append(" / ");
            displayName.append(option2Value);
        }
        if (option3Value != null) {
            if (displayName.length() > 0) displayName.append(" / ");
            displayName.append(option3Value);
        }
        
        return displayName.length() > 0 ? displayName.toString() : name;
    }

    public BigDecimal getDiscountPercentage() {
        if (comparePrice == null || comparePrice.compareTo(price) <= 0) {
            return BigDecimal.ZERO;
        }
        return comparePrice.subtract(price)
                .divide(comparePrice, 2, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }
}