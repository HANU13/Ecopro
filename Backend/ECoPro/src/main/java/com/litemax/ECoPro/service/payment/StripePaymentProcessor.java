package com.litemax.ECoPro.service.payment;


import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.litemax.ECoPro.controller.product.PaymentGatewayRequest;
import com.litemax.ECoPro.controller.product.PaymentGatewayResult;
import com.litemax.ECoPro.controller.product.RefundRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripePaymentProcessor implements PaymentProcessor {
    
    @Value("${app.payment.gateway.stripe.secret-key}")
    private String stripeSecretKey;
    
    @Value("${app.payment.gateway.stripe.api-url:https://api.stripe.com/v1}")
    private String stripeApiUrl;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Override
    public PaymentGatewayResult processPayment(PaymentGatewayRequest request) {
        log.info("Processing Stripe payment for amount: {} {}", request.getAmount(), request.getCurrency());
        
        try {
            // Create payment intent
            HttpHeaders headers = createStripeHeaders();
            
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("amount", String.valueOf(request.getAmount().multiply(BigDecimal.valueOf(100)).intValue())); // Convert to cents
            body.add("currency", request.getCurrency().toLowerCase());
            body.add("payment_method", request.getPaymentMethodId());
            body.add("confirm", "true");
            body.add("return_url", request.getReturnUrl());
            
            if (request.getCustomerId() != null) {
                body.add("customer", request.getCustomerId());
            }
            
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
            
            String requestJson = objectMapper.writeValueAsString(body.toSingleValueMap());
            log.debug("Stripe payment request: {}", requestJson);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                stripeApiUrl + "/payment_intents", entity, String.class);
            
            log.debug("Stripe payment response: {}", response.getBody());
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                
                String status = responseJson.get("status").asText();
                String paymentIntentId = responseJson.get("id").asText();
                
                if ("succeeded".equals(status)) {
                    log.info("Stripe payment succeeded: {}", paymentIntentId);
                    return PaymentGatewayResult.success(paymentIntentId, requestJson, response.getBody());
                } else if ("requires_action".equals(status)) {
                    log.info("Stripe payment requires additional action: {}", paymentIntentId);
                    String clientSecret = responseJson.get("client_secret").asText();
                    return PaymentGatewayResult.requiresAction(paymentIntentId, clientSecret, requestJson, response.getBody());
                } else {
                    log.error("Stripe payment failed with status: {}", status);
                    return PaymentGatewayResult.failed("Payment failed with status: " + status, requestJson, response.getBody());
                }
            } else {
                log.error("Stripe API error: {} - {}", response.getStatusCode(), response.getBody());
                return PaymentGatewayResult.failed("Stripe API error: " + response.getStatusCode(), requestJson, response.getBody());
            }
            
        } catch (Exception e) {
            log.error("Error processing Stripe payment: {}", e.getMessage(), e);
            return PaymentGatewayResult.failed("Stripe payment processing error: " + e.getMessage());
        }
    }
    
    @Override
    public PaymentGatewayResult refundPayment(RefundRequest request) {
        log.info("Processing Stripe refund for payment intent: {} amount: {}", 
                request.getOriginalTransactionId(), request.getAmount());
        
        try {
            HttpHeaders headers = createStripeHeaders();
            
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("payment_intent", request.getOriginalTransactionId());
            
            if (request.getAmount() != null) {
                body.add("amount", String.valueOf(request.getAmount().multiply(BigDecimal.valueOf(100)).intValue()));
            }
            
            if (request.getReason() != null) {
                body.add("reason", request.getReason());
            }
            
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
            
            String requestJson = objectMapper.writeValueAsString(body.toSingleValueMap());
            log.debug("Stripe refund request: {}", requestJson);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                stripeApiUrl + "/refunds", entity, String.class);
            
            log.debug("Stripe refund response: {}", response.getBody());
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                String status = responseJson.get("status").asText();
                String refundId = responseJson.get("id").asText();
                
                if ("succeeded".equals(status)) {
                    log.info("Stripe refund succeeded: {}", refundId);
                    return PaymentGatewayResult.success(refundId, requestJson, response.getBody());
                } else {
                    log.error("Stripe refund failed with status: {}", status);
                    return PaymentGatewayResult.failed("Refund failed with status: " + status, requestJson, response.getBody());
                }
            } else {
                log.error("Stripe refund API error: {} - {}", response.getStatusCode(), response.getBody());
                return PaymentGatewayResult.failed("Stripe refund API error: " + response.getStatusCode(), requestJson, response.getBody());
            }
            
        } catch (Exception e) {
            log.error("Error processing Stripe refund: {}", e.getMessage(), e);
            return PaymentGatewayResult.failed("Stripe refund processing error: " + e.getMessage());
        }
    }
    
    private HttpHeaders createStripeHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        String auth = stripeSecretKey + ":";
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encodedAuth);
        
        return headers;
    }
}

