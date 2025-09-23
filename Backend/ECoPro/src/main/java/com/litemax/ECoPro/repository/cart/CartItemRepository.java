package com.litemax.ECoPro.repository.cart;

import com.litemax.ECoPro.entity.cart.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByCartId(Long cartId);
    List<CartItem> findByProductId(Long productId);
    
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);
    Optional<CartItem> findByCartIdAndProductIdAndVariantId(Long cartId, Long productId, Long variantId);
    
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.user.id = :userId")
    List<CartItem> findByUserId(@Param("userId") Long userId);
    
    @Query("SELECT ci FROM CartItem ci WHERE ci.product.id = :productId AND ci.variant.id = :variantId")
    List<CartItem> findByProductIdAndVariantId(@Param("productId") Long productId, @Param("variantId") Long variantId);
    
    long countByCartId(Long cartId);
    long countByProductId(Long productId);
    
    @Query("SELECT SUM(ci.quantity) FROM CartItem ci WHERE ci.cart.id = :cartId")
    Integer sumQuantityByCartId(@Param("cartId") Long cartId);
    
    void deleteByCartId(Long cartId);
    void deleteByProductId(Long productId);
    void deleteByCartIdAndProductId(Long cartId, Long productId);
}