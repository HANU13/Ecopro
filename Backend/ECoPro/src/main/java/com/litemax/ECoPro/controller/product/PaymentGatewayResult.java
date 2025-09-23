package com.litemax.ECoPro.controller.product;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class PaymentGatewayResult {
    
    private boolean success;
    private String transactionId;
    private String errorMessage;
    private String clientSecret; // For actions requiring additional user input
    private String gatewayRequest;
    private String gatewayResponse;
    private PaymentGatewayStatus status;
    
    public enum PaymentGatewayStatus {
        SUCCESS, FAILED, REQUIRES_ACTION, PENDING
    }
    
    public static PaymentGatewayResult success(String transactionId, String request, String response) {
        return new PaymentGatewayResult(true, transactionId, null, null, request, response, PaymentGatewayStatus.SUCCESS);
    }
    
    public static PaymentGatewayResult failed(String errorMessage) {
        return new PaymentGatewayResult(false, null, errorMessage, null, null, null, PaymentGatewayStatus.FAILED);
    }
    
    public static PaymentGatewayResult failed(String errorMessage, String request, String response) {
        return new PaymentGatewayResult(false, null, errorMessage, null, request, response, PaymentGatewayStatus.FAILED);
    }
    
    public static PaymentGatewayResult requiresAction(String transactionId, String clientSecret, String request, String response) {
        return new PaymentGatewayResult(false, transactionId, null, clientSecret, request, response, PaymentGatewayStatus.REQUIRES_ACTION);
    }
}
