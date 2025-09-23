package com.litemax.ECoPro.entity.product;

import com.litemax.ECoPro.entity.auth.User;
import com.litemax.ECoPro.entity.order.Order;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "reviews",
       indexes = {
           @Index(name = "idx_review_product", columnList = "product_id"),
           @Index(name = "idx_review_user", columnList = "user_id"),
           @Index(name = "idx_review_rating", columnList = "rating"),
           @Index(name = "idx_review_status", columnList = "status"),
           @Index(name = "idx_review_created", columnList = "createdAt")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_user_product_order", columnNames = {"user_id", "product_id", "order_id"})
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"user", "product", "order"})
@EqualsAndHashCode(exclude = {"user", "product", "order", "reviewImages", "reviewHelpful"})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Review {
    
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
    @JoinColumn(name = "order_id", nullable = false)
    private Order order; // Ensures user purchased the product
    
    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    @Column(nullable = false)
    private Integer rating;
    
    @Size(max = 100, message = "Title must not exceed 100 characters")
    @Column(length = 100)
    private String title;
    
    @Size(min = 10, max = 2000, message = "Review text must be between 10 and 2000 characters")
    @Column(length = 2000)
    private String reviewText;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewStatus status = ReviewStatus.PENDING;
    
    @Column(nullable = false)
    private Boolean isVerifiedPurchase = true; // Since order is required
    
    @Column(nullable = false)
    private Boolean isAnonymous = false;
    
    // Review quality metrics
    @Column(nullable = false)
    private Integer helpfulCount = 0;
    
    @Column(nullable = false)
    private Integer unhelpfulCount = 0;
    
    @Column(nullable = false)
    private Integer reportedCount = 0;
    
    // Moderation
    @Size(max = 500)
    private String moderatorNotes;
    
    @Column
    private Long moderatedBy; // User ID who moderated
    
    @Column
    private LocalDateTime moderatedAt;
    
    // Response from seller/admin
    @Size(max = 1000, message = "Response must not exceed 1000 characters")
    @Column(length = 1000)
    private String sellerResponse;
    
    @Column
    private Long sellerResponseBy; // User ID who responded
    
    @Column
    private LocalDateTime sellerResponseAt;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // Relationships
    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReviewImage> reviewImages;
    
    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReviewHelpful> reviewHelpful;
    
    public enum ReviewStatus {
        PENDING, APPROVED, REJECTED, FLAGGED, HIDDEN
    }
    
    // Utility methods
    public double getHelpfulnessRatio() {
        int totalVotes = helpfulCount + unhelpfulCount;
        if (totalVotes == 0) return 0.0;
        return (double) helpfulCount / totalVotes;
    }
    
    public boolean isHighQuality() {
        return reviewText != null && 
               reviewText.length() >= 50 && 
               helpfulCount >= 5 && 
               getHelpfulnessRatio() >= 0.7;
    }
    
    public boolean needsModeration() {
        return status == ReviewStatus.PENDING || 
               reportedCount >= 3 || 
               (reviewText != null && reviewText.toLowerCase().contains("spam"));
    }
}
