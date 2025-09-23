package com.litemax.ECoPro.repository.cart;

import com.litemax.ECoPro.entity.cart.Wishlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    List<Wishlist> findByUserId(Long userId);
    Page<Wishlist> findByUserId(Long userId, Pageable pageable);
    List<Wishlist> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Wishlist> findByUserIdOrderByPriorityDescCreatedAtDesc(Long userId);
    
    Optional<Wishlist> findByUserIdAndProductId(Long userId, Long productId);
    Optional<Wishlist> findByUserIdAndProductIdAndVariantId(Long userId, Long productId, Long variantId);
    
    List<Wishlist> findByProductId(Long productId);
    List<Wishlist> findByProductIdAndVariantId(Long productId, Long variantId);
    
    List<Wishlist> findByUserIdAndIsPublicTrue(Long userId);
    List<Wishlist> findByIsPublicTrue();
    
    @Query("SELECT w FROM Wishlist w WHERE w.user.id = :userId AND w.priority = :priority ORDER BY w.createdAt DESC")
    List<Wishlist> findByUserIdAndPriority(@Param("userId") Long userId, @Param("priority") Integer priority);
    
    boolean existsByUserIdAndProductId(Long userId, Long productId);
    boolean existsByUserIdAndProductIdAndVariantId(Long userId, Long productId, Long variantId);
    
    long countByUserId(Long userId);
    long countByProductId(Long productId);
    long countByIsPublicTrue();
    
    @Query("SELECT COUNT(w) FROM Wishlist w WHERE w.product.status = 'ACTIVE'")
    long countActiveProductWishlists();
    
    void deleteByUserId(Long userId);
    void deleteByProductId(Long productId);
    void deleteByUserIdAndProductId(Long userId, Long productId);
    void deleteByUserIdAndProductIdAndVariantId(Long userId, Long productId, Long variantId);
}