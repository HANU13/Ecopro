package com.litemax.ECoPro.repository.cart;

import com.litemax.ECoPro.entity.cart.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUserId(Long userId);
    Optional<Cart> findBySessionId(String sessionId);
    Optional<Cart> findByUserIdAndStatus(Long userId, Cart.CartStatus status);
    
    List<Cart> findByStatus(Cart.CartStatus status);
    List<Cart> findByExpiresAtBefore(LocalDateTime dateTime);
    
    @Query("SELECT c FROM Cart c WHERE c.status = 'ACTIVE' AND c.updatedAt < :cutoffDate")
    List<Cart> findAbandonedCarts(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Modifying
    @Query("UPDATE Cart c SET c.status = 'EXPIRED' WHERE c.expiresAt < :now")
    void expireOldCarts(@Param("now") LocalDateTime now);
    
    @Modifying
    @Query("UPDATE Cart c SET c.status = 'ABANDONED' WHERE c.status = 'ACTIVE' AND c.updatedAt < :cutoffDate")
    void markCartsAsAbandoned(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    boolean existsByUserId(Long userId);
    boolean existsBySessionId(String sessionId);
    
    long countByStatus(Cart.CartStatus status);
    long countByUserId(Long userId);
    
    @Query("SELECT COUNT(c) FROM Cart c WHERE c.itemsCount > 0 AND c.status = 'ACTIVE'")
    long countActiveCartsWithItems();
    
    void deleteByUserId(Long userId);
    void deleteBySessionId(String sessionId);
}