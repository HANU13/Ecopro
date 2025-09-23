package com.litemax.ECoPro.repository.order;

import com.litemax.ECoPro.entity.order.Payment;
import com.litemax.ECoPro.entity.order.Payment.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    Optional<Payment> findByPaymentReference(String paymentReference);
    
    List<Payment> findByOrderId(Long orderId);
    
    Page<Payment> findByUserId(Long userId, Pageable pageable);
    
    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);
    
    Optional<Payment> findByGatewayTransactionId(String gatewayTransactionId);
}