package com.litemax.ECoPro.entity.cart;


import com.litemax.ECoPro.entity.auth.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "session_id")
    private String sessionId; // For guest users

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CartStatus status = CartStatus.ACTIVE;

    @Column(name = "items_count")
    private Integer itemsCount = 0;

    @Column(name = "subtotal", precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", precision = 12, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "coupon_code")
    private String couponCode;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CartItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CartDiscount> discounts = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum CartStatus {
        ACTIVE, ABANDONED, CONVERTED, EXPIRED
    }

    // Helper methods
    public void addItem(CartItem item) {
        this.items.add(item);
        item.setCart(this);
        recalculateCart();
    }

    public void removeItem(CartItem item) {
        this.items.remove(item);
        item.setCart(null);
        recalculateCart();
    }

    public void clearItems() {
        this.items.clear();
        recalculateCart();
    }

    public void addDiscount(CartDiscount discount) {
        this.discounts.add(discount);
        discount.setCart(this);
        recalculateCart();
    }

    public void removeDiscount(CartDiscount discount) {
        this.discounts.remove(discount);
        discount.setCart(null);
        recalculateCart();
    }

    public void recalculateCart() {
        // Calculate items count
        this.itemsCount = items.size();

        // Calculate subtotal
        this.subtotal = items.stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate discount amount
        this.discountAmount = discounts.stream()
                .map(CartDiscount::getDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate tax amount (simplified - would be more complex in real scenarios)
        this.taxAmount = this.subtotal.subtract(this.discountAmount)
                .multiply(new BigDecimal("0.08")); // 8% tax rate

        // Calculate total
        this.totalAmount = this.subtotal
                .subtract(this.discountAmount)
                .add(this.taxAmount);

        // Ensure totals are not negative
        if (this.totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            this.totalAmount = BigDecimal.ZERO;
        }
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public CartItem findItemByProductId(Long productId) {
        return items.stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElse(null);
    }

    public CartItem findItemByVariantId(Long variantId) {
        return items.stream()
                .filter(item -> item.getVariant() != null && 
                               item.getVariant().getId().equals(variantId))
                .findFirst()
                .orElse(null);
    }

    public BigDecimal getSavingsAmount() {
        return items.stream()
                .map(CartItem::getSavingsAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(discountAmount);
    }
}