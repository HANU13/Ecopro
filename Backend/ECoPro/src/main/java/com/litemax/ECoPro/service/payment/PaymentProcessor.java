package com.litemax.ECoPro.service.payment;

import com.litemax.ECoPro.controller.product.PaymentGatewayRequest;
import com.litemax.ECoPro.controller.product.PaymentGatewayResult;
import com.litemax.ECoPro.controller.product.RefundRequest;

public interface PaymentProcessor {
    PaymentGatewayResult processPayment(PaymentGatewayRequest request);
    PaymentGatewayResult refundPayment(RefundRequest request);
}