package com.litemax.ECoPro.controller.order;

import com.litemax.ECoPro.dto.order.PaymentResponse;
import com.litemax.ECoPro.entity.order.Payment.PaymentMethod;
import com.litemax.ECoPro.entity.order.Payment.PaymentStatus;
import com.litemax.ECoPro.service.order.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    // Customer APIs
    @PostMapping("/initiate")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PaymentResponse> initiatePayment(@RequestParam Long orderId,
                                                           @RequestParam PaymentMethod paymentMethod,
                                                           Authentication authentication) {
        log.info("Initiating payment for order: {} using method: {} by user: {}",
                orderId, paymentMethod, authentication.getName());

        Long userId = getUserIdFromAuthentication(authentication);
        PaymentResponse payment = paymentService.initiatePayment(orderId, paymentMethod, userId);

        log.info("Payment initiated successfully with reference: {}", payment.getPaymentReference());
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }

    @PostMapping("/process")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PaymentResponse> processPayment(@RequestParam String paymentReference,
                                                          @RequestParam String gatewayTransactionId,
                                                          Authentication authentication) {
        log.info("Processing payment: {} with gateway transaction: {}", paymentReference, gatewayTransactionId);

        PaymentResponse payment = paymentService.processPayment(paymentReference, gatewayTransactionId);

        log.info("Payment processed with status: {}", payment.getStatus());
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/reference/{paymentReference}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PaymentResponse> getPaymentByReference(@PathVariable String paymentReference,
                                                                 Authentication authentication) {
        log.info("Fetching payment by reference: {} for user: {}", paymentReference, authentication.getName());

        Long userId = getUserIdFromAuthentication(authentication);
        PaymentResponse payment = paymentService.getPaymentByReference(paymentReference, userId);

        return ResponseEntity.ok(payment);
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<PaymentResponse>> getOrderPayments(@PathVariable Long orderId,
                                                                  Authentication authentication) {
        log.info("Fetching payments for order: {} by user: {}", orderId, authentication.getName());

        Long userId = getUserIdFromAuthentication(authentication);
        List<PaymentResponse> payments = paymentService.getOrderPayments(orderId, userId);

        log.debug("Found {} payments for order: {}", payments.size(), orderId);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/my-payments")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Page<PaymentResponse>> getMyPayments(@RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "10") int size,
                                                               Authentication authentication) {
        log.info("Fetching payments for user: {}", authentication.getName());

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Long userId = getUserIdFromAuthentication(authentication);
        Page<PaymentResponse> payments = paymentService.getUserPayments(userId, pageable);

        return ResponseEntity.ok(payments);
    }

    // Admin APIs
    @PostMapping("/admin/{paymentId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentResponse> refundPayment(@PathVariable Long paymentId,
                                                         @RequestParam @NotNull @DecimalMin("0.01") BigDecimal refundAmount,
                                                         @RequestParam String reason,
                                                         Authentication authentication) {
        log.info("Processing refund for payment: {} amount: {} by admin: {}",
                paymentId, refundAmount, authentication.getName());

        Long adminUserId = getUserIdFromAuthentication(authentication);
        PaymentResponse payment = paymentService.refundPayment(paymentId, refundAmount, reason, adminUserId);

        log.info("Refund processed successfully for payment: {}", paymentId);
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/admin/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<PaymentResponse>> getPaymentsByStatus(@PathVariable PaymentStatus status,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "20") int size) {
        log.info("Admin fetching payments with status: {}", status);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PaymentResponse> payments = paymentService.getPaymentsByStatus(status, pageable);

        return ResponseEntity.ok(payments);
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        return Long.parseLong(authentication.getName()); // Adjust as needed
    }
}