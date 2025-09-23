package com.litemax.ECoPro.repository.order;

import com.litemax.ECoPro.entity.order.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    
    List<OrderItem> findByOrderId(Long orderId);
    
    @Query("SELECT oi FROM OrderItem oi WHERE oi.product.id = :productId")
    List<OrderItem> findByProductId(@Param("productId") Long productId);
    
    @Query("SELECT oi FROM OrderItem oi JOIN oi.order o WHERE o.user.id = :userId AND oi.product.id = :productId")
    List<OrderItem> findByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);
}