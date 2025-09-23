package com.litemax.ECoPro.controller.order;


import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.litemax.ECoPro.controller.product.PaymentGatewayRequest;
import com.litemax.ECoPro.controller.product.PaymentGatewayResult;
import com.litemax.ECoPro.controller.product.RefundRequest;
import com.litemax.ECoPro.service.payment.PaymentGatewayService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/payment-gateway")
@RequiredArgsConstructor
@Slf4j
public class PaymentGatewayController {
    
    private final PaymentGatewayService paymentGatewayService;
    
    @PostMapping("/process")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PaymentGatewayResult> processPayment(@Valid @RequestBody PaymentGatewayRequest request,
                                                             Authentication authentication) {
        log.info("Processing payment through gateway: {} for user: {}", 
                request.getGateway(), authentication.getName());
        
        // Add user context to request
        request.setUserId(getUserIdFromAuthentication(authentication));
        
        PaymentGatewayResult result = paymentGatewayService.processPayment(request);
        
        log.info("Payment processing result: {}", result.isSuccess() ? "SUCCESS" : "FAILED");
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentGatewayResult> processRefund(@Valid @RequestBody RefundRequest request,
                                                            Authentication authentication) {
        log.info("Processing refund through gateway: {} by admin: {}", 
                request.getGateway(), authentication.getName());
        
        PaymentGatewayResult result = paymentGatewayService.refundPayment(request);
        
        log.info("Refund processing result: {}", result.isSuccess() ? "SUCCESS" : "FAILED");
        return ResponseEntity.ok(result);
    }
    
    // Webhook endpoints for payment gateways
    @PostMapping("/webhook/stripe")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload,
                                                     @RequestHeader("Stripe-Signature") String signature) {
        log.info("Received Stripe webhook");
        
        // TODO: Implement Stripe webhook signature verification and processing
        // This would handle events like payment_intent.succeeded, payment_intent.payment_failed, etc.
        
        return ResponseEntity.ok("OK");
    }
    
    @PostMapping("/webhook/paypal")
    public ResponseEntity<String> handlePayPalWebhook(@RequestBody String payload,
                                                     @RequestHeader("PAYPAL-TRANSMISSION-ID") String transmissionId) {
        log.info("Received PayPal webhook");
        
        // TODO: Implement PayPal webhook verification and processing
        // This would handle events like PAYMENT.CAPTURE.COMPLETED, PAYMENT.CAPTURE.DENIED, etc.
        
        return ResponseEntity.ok("OK");
    }
    
    private Long getUserIdFromAuthentication(Authentication authentication) {
        return Long.parseLong(authentication.getName()); // Adjust as needed
    }
}
