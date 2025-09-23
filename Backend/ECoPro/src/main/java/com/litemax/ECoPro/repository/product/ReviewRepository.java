package com.litemax.ECoPro.repository.product;

import com.litemax.ECoPro.entity.order.Order;
import com.litemax.ECoPro.entity.product.Review;
import com.litemax.ECoPro.entity.product.Review.ReviewStatus;
import com.litemax.ECoPro.entity.auth.User;
import com.litemax.ECoPro.entity.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    // User can only have one review per product per order
    Optional<Review> findByUserAndProductAndOrder(User user, Product product, Order order);
    
    // Product reviews
    Page<Review> findByProductAndStatus(Product product, ReviewStatus status, Pageable pageable);
    
    Page<Review> findByProductAndStatusIn(Product product, List<ReviewStatus> statuses, Pageable pageable);
    
    @Query("SELECT r FROM Review r WHERE r.product = :product AND r.status IN :statuses ORDER BY r.helpfulCount DESC, r.createdAt DESC")
    Page<Review> findByProductAndStatusOrderByHelpfulness(@Param("product") Product product, 
                                                         @Param("statuses") List<ReviewStatus> statuses, 
                                                         Pageable pageable);
    
    // User reviews
    Page<Review> findByUserAndStatus(User user, ReviewStatus status, Pageable pageable);
    
    Page<Review> findByUser(User user, Pageable pageable);
    
    // Admin queries
    Page<Review> findByStatus(ReviewStatus status, Pageable pageable);
    
    @Query("SELECT r FROM Review r WHERE r.reportedCount >= :threshold")
    Page<Review> findReviewsNeedingModeration(@Param("threshold") Integer threshold, Pageable pageable);
    
    @Query("SELECT r FROM Review r WHERE r.createdAt BETWEEN :startDate AND :endDate")
    List<Review> findReviewsBetweenDates(@Param("startDate") LocalDateTime startDate, 
                                        @Param("endDate") LocalDateTime endDate);
    
    // Statistics
    @Query("SELECT COUNT(r) FROM Review r WHERE r.product = :product AND r.status = :status")
    Long countByProductAndStatus(@Param("product") Product product, @Param("status") ReviewStatus status);
    
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product = :product AND r.status = :status")
    Double getAverageRatingByProduct(@Param("product") Product product, @Param("status") ReviewStatus status);
    
    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.product = :product AND r.status = :status GROUP BY r.rating")
    List<Object[]> getRatingDistribution(@Param("product") Product product, @Param("status") ReviewStatus status);
    
    // Check if user purchased product
    @Query("SELECT COUNT(oi) > 0 FROM OrderItem oi WHERE oi.order.user = :user AND oi.product = :product AND oi.order.status = 'DELIVERED'")
    boolean hasUserPurchasedProduct(@Param("user") User user, @Param("product") Product product);
}