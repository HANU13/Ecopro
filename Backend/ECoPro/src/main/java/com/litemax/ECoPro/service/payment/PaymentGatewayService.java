package com.litemax.ECoPro.service.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.litemax.ECoPro.controller.product.PaymentGatewayRequest;
import com.litemax.ECoPro.controller.product.PaymentGatewayResult;
import com.litemax.ECoPro.controller.product.RefundRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentGatewayService {
    
    @Value("${app.payment.gateway.stripe.enabled:false}")
    private boolean stripeEnabled;
    
    @Value("${app.payment.gateway.paypal.enabled:false}")
    private boolean paypalEnabled;
    
    private final StripePaymentProcessor stripeProcessor;
    private final PayPalPaymentProcessor paypalProcessor;
    
    public PaymentGatewayResult processPayment(PaymentGatewayRequest request) {
        log.info("Processing payment through gateway: {} for amount: {}", 
                request.getGateway(), request.getAmount());
        
        try {
            switch (request.getGateway()) {
                case STRIPE:
                    if (!stripeEnabled) {
                        log.error("Stripe payment gateway is disabled");
                        return PaymentGatewayResult.failed("Stripe gateway is not available");
                    }
                    return stripeProcessor.processPayment(request);
                    
                case PAYPAL:
                    if (!paypalEnabled) {
                        log.error("PayPal payment gateway is disabled");
                        return PaymentGatewayResult.failed("PayPal gateway is not available");
                    }
                    return paypalProcessor.processPayment(request);
                    
                default:
                    log.error("Unsupported payment gateway: {}", request.getGateway());
                    return PaymentGatewayResult.failed("Unsupported payment gateway");
            }
        } catch (Exception e) {
            log.error("Error processing payment through gateway {}: {}", request.getGateway(), e.getMessage(), e);
            return PaymentGatewayResult.failed("Payment processing failed: " + e.getMessage());
        }
    }
    
    public PaymentGatewayResult refundPayment(RefundRequest request) {
        log.info("Processing refund through gateway: {} for amount: {}", 
                request.getGateway(), request.getAmount());
        
        try {
            switch (request.getGateway()) {
                case STRIPE:
                    return stripeProcessor.refundPayment(request);
                case PAYPAL:
                    return paypalProcessor.refundPayment(request);
                default:
                    log.error("Unsupported gateway for refund: {}", request.getGateway());
                    return PaymentGatewayResult.failed("Unsupported gateway for refund");
            }
        } catch (Exception e) {
            log.error("Error processing refund through gateway {}: {}", request.getGateway(), e.getMessage(), e);
            return PaymentGatewayResult.failed("Refund processing failed: " + e.getMessage());
        }
    }
}