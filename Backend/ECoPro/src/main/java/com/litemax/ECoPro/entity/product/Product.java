package com.litemax.ECoPro.entity.product;

import com.litemax.ECoPro.entity.auth.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "short_description")
    private String shortDescription;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "compare_price", precision = 10, scale = 2)
    private BigDecimal comparePrice;

    @Column(name = "cost_price", precision = 10, scale = 2)
    private BigDecimal costPrice;

    @Column(name = "track_inventory")
    private boolean trackInventory = true;

    @Column(name = "inventory_quantity")
    private Integer inventoryQuantity = 0;

    @Column(name = "low_stock_threshold")
    private Integer lowStockThreshold = 5;

    @Column(name = "allow_backorders")
    private boolean allowBackorders = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status = ProductStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "inventory_status")
    private InventoryStatus inventoryStatus = InventoryStatus.IN_STOCK;

    @Column(name = "is_featured")
    private boolean featured = false;

    @Column(name = "is_digital")
    private boolean digital = false;

    @Column(name = "requires_shipping")
    private boolean requiresShipping = true;

    @Column(name = "weight")
    private BigDecimal weight;

    @Column(name = "dimensions")
    private String dimensions; // JSON or formatted string like "10x20x5"

    @Column(name = "meta_title")
    private String metaTitle;

    @Column(name = "meta_description")
    private String metaDescription;

    @Column(name = "meta_keywords")
    private String metaKeywords;

    @Column(name = "tags")
    private String tags; // Comma-separated tags

    @Column(name = "vendor")
    private String vendor;

    @Column(name = "barcode")
    private String barcode;

    @Column(name = "hsn_code")
    private String hsnCode;

    @Column(name = "tax_percentage", precision = 5, scale = 2)
    private BigDecimal taxPercentage = BigDecimal.ZERO;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "view_count")
    private Long viewCount = 0L;

    @Column(name = "rating", precision = 3, scale = 2)
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(name = "review_count")
    private Integer reviewCount = 0;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "product_categories",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new HashSet<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProductMedia> media = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProductAttribute> attributes = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    // Enums
    public enum ProductStatus {
        DRAFT, ACTIVE, INACTIVE, ARCHIVED, OUT_OF_STOCK
    }

    public enum InventoryStatus {
        IN_STOCK, LOW_STOCK, OUT_OF_STOCK, BACKORDER, DISCONTINUED
    }

    // Helper Methods
    public void addCategory(Category category) {
        this.categories.add(category);
    }

    public void removeCategory(Category category) {
        this.categories.remove(category);
    }

    public void addMedia(ProductMedia media) {
        this.media.add(media);
        media.setProduct(this);
    }

    public void removeMedia(ProductMedia media) {
        this.media.remove(media);
        media.setProduct(null);
    }

    public void addVariant(ProductVariant variant) {
        this.variants.add(variant);
        variant.setProduct(this);
    }

    public void removeVariant(ProductVariant variant) {
        this.variants.remove(variant);
        variant.setProduct(null);
    }

    public void addAttribute(ProductAttribute attribute) {
        this.attributes.add(attribute);
        attribute.setProduct(this);
    }

    public void removeAttribute(ProductAttribute attribute) {
        this.attributes.remove(attribute);
        attribute.setProduct(null);
    }

    public boolean isInStock() {
        if (!trackInventory) return true;
        return inventoryQuantity != null && inventoryQuantity > 0;
    }

    public boolean isLowStock() {
        if (!trackInventory) return false;
        return inventoryQuantity != null && inventoryQuantity <= lowStockThreshold;
    }

    public BigDecimal getDiscountPercentage() {
        if (comparePrice == null || price == null || comparePrice.compareTo(price) <= 0) {
            return BigDecimal.ZERO;
        }
        return comparePrice.subtract(price)
                .divide(comparePrice, 2, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    public String getFormattedDimensions() {
        return dimensions != null ? dimensions : "Not specified";
    }

    public List<String> getTagsList() {
        if (tags == null || tags.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(tags.split(","));
    }

    public ProductMedia getPrimaryImage() {
        return media.stream()
                .filter(ProductMedia::isPrimary)
                .findFirst()
                .orElse(media.isEmpty() ? null : media.get(0));
    }
}