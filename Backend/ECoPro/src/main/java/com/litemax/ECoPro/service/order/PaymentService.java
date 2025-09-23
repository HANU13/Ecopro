package com.litemax.ECoPro.service.order;

import com.litemax.ECoPro.dto.order.PaymentResponse;
import com.litemax.ECoPro.entity.order.Order;
import com.litemax.ECoPro.entity.order.Payment;
import com.litemax.ECoPro.entity.order.Payment.PaymentMethod;
import com.litemax.ECoPro.entity.order.Payment.PaymentStatus;
import com.litemax.ECoPro.exception.ResourceNotFoundException;
import com.litemax.ECoPro.exception.ValidationException;
import com.litemax.ECoPro.repository.order.PaymentRepository;
import com.litemax.ECoPro.repository.order.OrderRepository;
import com.litemax.ECoPro.service.UserService;
import com.litemax.ECoPro.util.MapperUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserService userService;
    private final MapperUtil mapperUtil;
    
    @Transactional
    public PaymentResponse initiatePayment(Long orderId, PaymentMethod paymentMethod, Long userId) {
        log.info("Initiating payment for order ID: {} using method: {} by user: {}", 
                orderId, paymentMethod, userId);
        
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> {
                log.error("Order not found with ID: {}", orderId);
                return new ResourceNotFoundException("Order not found with ID: " + orderId);
            });
        
        // Validate user has access to this order
        if (!order.getUser().getId().equals(userId)) {
            log.error("Unauthorized payment initiation: User {} for order {}", userId, orderId);
            throw new ValidationException("Unauthorized access to order");
        }
        
        // Check if order is in valid state for payment
        if (order.getStatus() != Order.OrderStatus.PLACED && order.getStatus() != Order.OrderStatus.CONFIRMED) {
            log.error("Invalid order status for payment: {} for order: {}", order.getStatus(), order.getOrderNumber());
            throw new ValidationException("Order is not in valid state for payment");
        }
        
        // Create payment record
        Payment payment = new Payment();
        payment.setPaymentReference(generatePaymentReference());
        payment.setOrder(order);
        payment.setUser(order.getUser());
        payment.setPaymentMethod(paymentMethod);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(order.getTotalAmount());
        
        payment = paymentRepository.save(payment);
        log.info("Payment initiated with reference: {} for amount: {}", 
                payment.getPaymentReference(), payment.getAmount());
        
        // TODO: Integrate with actual payment gateway (Stripe/PayPal)
        // For now, we'll simulate payment processing
        
        return mapperUtil.mapToPaymentResponse(payment);
    }
    
    @Transactional
    public PaymentResponse processPayment(String paymentReference, String gatewayTransactionId) {
        log.info("Processing payment with reference: {} and gateway transaction: {}", 
                paymentReference, gatewayTransactionId);
        
        Payment payment = paymentRepository.findByPaymentReference(paymentReference)
            .orElseThrow(() -> {
                log.error("Payment not found with reference: {}", paymentReference);
                return new ResourceNotFoundException("Payment not found with reference: " + paymentReference);
            });
        
        // Update payment status
        payment.setStatus(PaymentStatus.PROCESSING);
        payment.setGatewayTransactionId(gatewayTransactionId);
        payment.setProcessedAt(LocalDateTime.now());
        
        // TODO: Call payment gateway API to verify transaction
        // Simulate successful payment
        boolean paymentSuccessful = simulatePaymentProcessing();
        
        if (paymentSuccessful) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setGatewayResponse("Payment completed successfully");
            
            // Update order status
            Order order = payment.getOrder();
            order.setStatus(Order.OrderStatus.CONFIRMED);
            orderRepository.save(order);
            
            log.info("Payment completed successfully: {} for order: {}", 
                    paymentReference, order.getOrderNumber());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Payment gateway declined the transaction");
            payment.setGatewayResponse("Transaction declined");
            
            log.error("Payment failed: {} with reason: {}", paymentReference, payment.getFailureReason());
        }
        
        payment = paymentRepository.save(payment);
        
        // TODO: Send payment confirmation/failure notification
        
        return mapperUtil.mapToPaymentResponse(payment);
    }
    
    @Transactional
    public PaymentResponse refundPayment(Long paymentId, BigDecimal refundAmount, String reason, Long adminUserId) {
        log.info("Processing refund for payment ID: {} amount: {} by admin: {}", 
                paymentId, refundAmount, adminUserId);
        
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> {
                log.error("Payment not found with ID: {}", paymentId);
                return new ResourceNotFoundException("Payment not found with ID: " + paymentId);
            });
        
        // Validate payment can be refunded
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            log.error("Payment cannot be refunded in current status: {} for payment: {}", 
                     payment.getStatus(), paymentId);
            throw new ValidationException("Payment cannot be refunded in current status: " + payment.getStatus());
        }
        
        // Validate refund amount
        if (refundAmount.compareTo(payment.getAmount()) > 0) {
            log.error("Refund amount {} exceeds payment amount {} for payment: {}", 
                     refundAmount, payment.getAmount(), paymentId);
            throw new ValidationException("Refund amount cannot exceed payment amount");
        }
        
        // TODO: Call payment gateway API to process refund
        boolean refundSuccessful = simulateRefundProcessing();
        
        if (refundSuccessful) {
            if (refundAmount.compareTo(payment.getAmount()) == 0) {
                payment.setStatus(PaymentStatus.REFUNDED);
            } else {
                payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
            }
            
            payment.setGatewayResponse("Refund processed successfully. Amount: " + refundAmount + ". Reason: " + reason);
            
            // Update order status
            Order order = payment.getOrder();
            if (refundAmount.compareTo(payment.getAmount()) == 0) {
                order.setStatus(Order.OrderStatus.REFUNDED);
            }
            orderRepository.save(order);
            
            log.info("Refund processed successfully: Payment {} refund amount: {}", paymentId, refundAmount);
        } else {
            log.error("Refund processing failed for payment: {}", paymentId);
            throw new ValidationException("Refund processing failed");
        }
        
        payment = paymentRepository.save(payment);
        
        // TODO: Send refund confirmation notification
        
        return mapperUtil.mapToPaymentResponse(payment);
    }
    
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByReference(String paymentReference, Long userId) {
        log.info("Fetching payment by reference: {} for user: {}", paymentReference, userId);
        
        Payment payment = paymentRepository.findByPaymentReference(paymentReference)
            .orElseThrow(() -> {
                log.error("Payment not found with reference: {}", paymentReference);
                return new ResourceNotFoundException("Payment not found with reference: " + paymentReference);
            });
        
        // Validate user has access to this payment
        if (!payment.getUser().getId().equals(userId)) {
            log.error("Unauthorized payment access: User {} for payment {}", userId, paymentReference);
            throw new ValidationException("Unauthorized access to payment");
        }
        
        return mapperUtil.mapToPaymentResponse(payment);
    }
    
    @Transactional(readOnly = true)
    public List<PaymentResponse> getOrderPayments(Long orderId, Long userId) {
        log.info("Fetching payments for order ID: {} by user: {}", orderId, userId);
        
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> {
                log.error("Order not found with ID: {}", orderId);
                return new ResourceNotFoundException("Order not found with ID: " + orderId);
            });
        
        // Validate user has access to this order
        if (!order.getUser().getId().equals(userId)) {
            log.error("Unauthorized order access: User {} for order {}", userId, orderId);
            throw new ValidationException("Unauthorized access to order");
        }
        
        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        log.debug("Found {} payments for order: {}", payments.size(), orderId);
        
        return payments.stream()
            .map(mapperUtil::mapToPaymentResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getUserPayments(Long userId, Pageable pageable) {
        log.info("Fetching payments for user ID: {}", userId);
        
        Page<Payment> payments = paymentRepository.findByUserId(userId, pageable);
        log.debug("Found {} payments for user: {}", payments.getTotalElements(), userId);
        
        return payments.map(mapperUtil::mapToPaymentResponse);
    }
    
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsByStatus(PaymentStatus status, Pageable pageable) {
        log.info("Fetching payments with status: {}", status);
        
        Page<Payment> payments = paymentRepository.findByStatus(status, pageable);
        log.debug("Found {} payments with status: {}", payments.getTotalElements(), status);
        
        return payments.map(mapperUtil::mapToPaymentResponse);
    }
    
    private String generatePaymentReference() {
        return "PAY-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    private boolean simulatePaymentProcessing() {
        // Simulate 95% success rate
        return Math.random() < 0.95;
    }
    
    private boolean simulateRefundProcessing() {
        // Simulate 98% success rate for refunds
        return Math.random() < 0.98;
    }
}