package com.litemax.ECoPro.entity.product;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_ratings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProductRating {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;
    
    @DecimalMin(value = "0.0", message = "Average rating must be non-negative")
    @DecimalMax(value = "5.0", message = "Average rating cannot exceed 5.0")
    @Digits(integer = 1, fraction = 2, message = "Invalid average rating format")
    @Column(precision = 3, scale = 2)
    private BigDecimal averageRating = BigDecimal.ZERO;
    
    @Min(value = 0, message = "Total reviews must be non-negative")
    @Column(nullable = false)
    private Integer totalReviews = 0;
    
    // Rating distribution
    @Min(value = 0)
    @Column(nullable = false)
    private Integer rating1Count = 0;
    
    @Min(value = 0)
    @Column(nullable = false)
    private Integer rating2Count = 0;
    
    @Min(value = 0)
    @Column(nullable = false)
    private Integer rating3Count = 0;
    
    @Min(value = 0)
    @Column(nullable = false)
    private Integer rating4Count = 0;
    
    @Min(value = 0)
    @Column(nullable = false)
    private Integer rating5Count = 0;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // Utility methods
    public void updateRatingCounts(Integer rating, boolean isAdd) {
        int change = isAdd ? 1 : -1;
        
        switch (rating) {
            case 1: rating1Count = Math.max(0, rating1Count + change); break;
            case 2: rating2Count = Math.max(0, rating2Count + change); break;
            case 3: rating3Count = Math.max(0, rating3Count + change); break;
            case 4: rating4Count = Math.max(0, rating4Count + change); break;
            case 5: rating5Count = Math.max(0, rating5Count + change); break;
        }
        
        totalReviews = Math.max(0, totalReviews + change);
        recalculateAverageRating();
    }
    
    public void recalculateAverageRating() {
        if (totalReviews == 0) {
            averageRating = BigDecimal.ZERO;
            return;
        }
        
        int totalRatingPoints = (rating1Count * 1) + (rating2Count * 2) + 
                               (rating3Count * 3) + (rating4Count * 4) + (rating5Count * 5);
        
        averageRating = BigDecimal.valueOf((double) totalRatingPoints / totalReviews)
                                 .setScale(2, BigDecimal.ROUND_HALF_UP);
    }
    
    public BigDecimal getRatingPercentage(int rating) {
        if (totalReviews == 0) return BigDecimal.ZERO;
        
        int count = 0;
        switch (rating) {
            case 1: count = rating1Count; break;
            case 2: count = rating2Count; break;
            case 3: count = rating3Count; break;
            case 4: count = rating4Count; break;
            case 5: count = rating5Count; break;
        }
        
        return BigDecimal.valueOf((double) count / totalReviews * 100)
                        .setScale(1, BigDecimal.ROUND_HALF_UP);
    }
}