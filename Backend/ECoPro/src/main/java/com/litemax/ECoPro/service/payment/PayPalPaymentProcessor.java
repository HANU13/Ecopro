package com.litemax.ECoPro.service.payment;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
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
public class PayPalPaymentProcessor implements PaymentProcessor {
    
    @Value("${app.payment.gateway.paypal.client-id}")
    private String paypalClientId;
    
    @Value("${app.payment.gateway.paypal.client-secret}")
    private String paypalClientSecret;
    
    @Value("${app.payment.gateway.paypal.api-url:https://api-m.sandbox.paypal.com}")
    private String paypalApiUrl;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Override
    public PaymentGatewayResult processPayment(PaymentGatewayRequest request) {
        log.info("Processing PayPal payment for amount: {} {}", request.getAmount(), request.getCurrency());
        
        try {
            // Get access token
            String accessToken = getAccessToken();
            if (accessToken == null) {
                return PaymentGatewayResult.failed("Failed to obtain PayPal access token");
            }
            
            // Create order
            HttpHeaders headers = createPayPalHeaders(accessToken);
            
            Map<String, Object> orderRequest = createPayPalOrderRequest(request);
            String requestJson = objectMapper.writeValueAsString(orderRequest);
            log.debug("PayPal order request: {}", requestJson);
            
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                paypalApiUrl + "/v2/checkout/orders", entity, String.class);
            
            log.debug("PayPal order response: {}", response.getBody());
            
            if (response.getStatusCode() == HttpStatus.CREATED) {
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                String orderId = responseJson.get("id").asText();
                String status = responseJson.get("status").asText();
                
                if ("CREATED".equals(status)) {
                    // Capture the order
                    return capturePayPalOrder(orderId, accessToken, requestJson, response.getBody());
                } else {
                    log.error("PayPal order creation failed with status: {}", status);
                    return PaymentGatewayResult.failed("PayPal order creation failed: " + status, requestJson, response.getBody());
                }
            } else {
                log.error("PayPal API error: {} - {}", response.getStatusCode(), response.getBody());
                return PaymentGatewayResult.failed("PayPal API error: " + response.getStatusCode(), requestJson, response.getBody());
            }
            
        } catch (Exception e) {
            log.error("Error processing PayPal payment: {}", e.getMessage(), e);
            return PaymentGatewayResult.failed("PayPal payment processing error: " + e.getMessage());
        }
    }
    
    @Override
    public PaymentGatewayResult refundPayment(RefundRequest request) {
        log.info("Processing PayPal refund for capture ID: {} amount: {}", 
                request.getOriginalTransactionId(), request.getAmount());
        
        try {
            String accessToken = getAccessToken();
            if (accessToken == null) {
                return PaymentGatewayResult.failed("Failed to obtain PayPal access token");
            }
            
            HttpHeaders headers = createPayPalHeaders(accessToken);
            
            Map<String, Object> refundRequest = new HashMap<>();
            if (request.getAmount() != null) {
                Map<String, String> amount = new HashMap<>();
                amount.put("currency_code", request.getCurrency());
                amount.put("value", request.getAmount().toString());
                refundRequest.put("amount", amount);
            }
            
            String requestJson = objectMapper.writeValueAsString(refundRequest);
            log.debug("PayPal refund request: {}", requestJson);
            
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                paypalApiUrl + "/v2/payments/captures/" + request.getOriginalTransactionId() + "/refund",
                entity, String.class);
            
            log.debug("PayPal refund response: {}", response.getBody());
            
            if (response.getStatusCode() == HttpStatus.CREATED) {
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                String refundId = responseJson.get("id").asText();
                String status = responseJson.get("status").asText();
                
                if ("COMPLETED".equals(status)) {
                    log.info("PayPal refund completed: {}", refundId);
                    return PaymentGatewayResult.success(refundId, requestJson, response.getBody());
                } else {
                    log.error("PayPal refund failed with status: {}", status);
                    return PaymentGatewayResult.failed("PayPal refund failed: " + status, requestJson, response.getBody());
                }
            } else {
                log.error("PayPal refund API error: {} - {}", response.getStatusCode(), response.getBody());
                return PaymentGatewayResult.failed("PayPal refund API error: " + response.getStatusCode(), requestJson, response.getBody());
            }
            
        } catch (Exception e) {
            log.error("Error processing PayPal refund: {}", e.getMessage(), e);
            return PaymentGatewayResult.failed("PayPal refund processing error: " + e.getMessage());
        }
    }
    
    private String getAccessToken() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            String auth = paypalClientId + ":" + paypalClientSecret;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + encodedAuth);
            
            String body = "grant_type=client_credentials";
            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                paypalApiUrl + "/v1/oauth2/token", entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                return responseJson.get("access_token").asText();
            }
            
        } catch (Exception e) {
            log.error("Error getting PayPal access token: {}", e.getMessage(), e);
        }
        
        return null;
    }
    
    private PaymentGatewayResult capturePayPalOrder(String orderId, String accessToken, String originalRequest, String originalResponse) {
        try {
            HttpHeaders headers = createPayPalHeaders(accessToken);
            HttpEntity<String> entity = new HttpEntity<>("{}", headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                paypalApiUrl + "/v2/checkout/orders/" + orderId + "/capture", entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.CREATED) {
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                String status = responseJson.get("status").asText();
                
                if ("COMPLETED".equals(status)) {
                    JsonNode captureDetails = responseJson.get("purchase_units").get(0).get("payments").get("captures").get(0);
                    String captureId = captureDetails.get("id").asText();
                    
                    log.info("PayPal payment captured successfully: {}", captureId);
                    return PaymentGatewayResult.success(captureId, originalRequest, response.getBody());
                } else {
                    log.error("PayPal order capture failed with status: {}", status);
                    return PaymentGatewayResult.failed("PayPal capture failed: " + status, originalRequest, response.getBody());
                }
            } else {
                log.error("PayPal capture API error: {} - {}", response.getStatusCode(), response.getBody());
                return PaymentGatewayResult.failed("PayPal capture API error: " + response.getStatusCode(), originalRequest, response.getBody());
            }
            
        } catch (Exception e) {
            log.error("Error capturing PayPal order: {}", e.getMessage(), e);
            return PaymentGatewayResult.failed("PayPal order capture error: " + e.getMessage());
        }
    }
    
    private HttpHeaders createPayPalHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + accessToken);
        return headers;
    }
    
    private Map<String, Object> createPayPalOrderRequest(PaymentGatewayRequest request) {
        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("intent", "CAPTURE");
        
        Map<String, String> amount = new HashMap<>();
        amount.put("currency_code", request.getCurrency());
        amount.put("value", request.getAmount().toString());
        
        Map<String, Object> purchaseUnit = new HashMap<>();
        purchaseUnit.put("amount", amount);
        
        orderRequest.put("purchase_units", new Object[]{purchaseUnit});
        
        return orderRequest;
    }
}
