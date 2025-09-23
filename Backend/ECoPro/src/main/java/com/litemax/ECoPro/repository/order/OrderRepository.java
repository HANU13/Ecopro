package com.litemax.ECoPro.repository.order;

import com.litemax.ECoPro.entity.auth.User;
import com.litemax.ECoPro.entity.order.Order;
import com.litemax.ECoPro.entity.order.Order.OrderStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
	
	@Query("SELECT COALESCE(SUM(o.totalAmount), 0) " +
		       "FROM Order o " +
		       "WHERE o.status IN :status")
    Double getTotalRevenueByStatus(@Param("status") List<Order.OrderStatus> status);

    Page<Order> findByStatus(Order.OrderStatus status, Pageable pageable);

    List<Order> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    Optional<Order> findByOrderNumber(String orderNumber);
    
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    List<Order> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, Order.OrderStatus status);
    
    Page<Order> findByStatusOrderByCreatedAtDesc(Order.OrderStatus status, Pageable pageable);
    
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate ORDER BY o.createdAt DESC")
    List<Order> findOrdersByDateRange(@Param("startDate") LocalDateTime startDate, 
                                     @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    Long countByStatus(@Param("status") Order.OrderStatus status);
    
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) " +
    	       "FROM Order o " +
    	       "WHERE o.status IN :statuses " +
    	       "AND o.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal calculateRevenueByStatusAndDateRange(@Param("statuses") List<Order.OrderStatus> statuses,
    	                                                @Param("startDate") LocalDateTime startDate,
    	                                                @Param("endDate") LocalDateTime endDate);

    
    Page<Order> findByUserAndStatus(User user, OrderStatus status, Pageable pageable);
}
