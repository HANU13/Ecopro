package com.litemax.ECoPro.dto.order;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;


@Data
public class CreateOrderRequest {

    @NotNull(message = "Cart ID is required")
    private Long cartId;

    @NotBlank(message = "Shipping address line 1 is required")
    @Size(max = 255, message = "Shipping address line 1 must not exceed 255 characters")
    private String shippingAddressLine1;

    @Size(max = 255, message = "Shipping address line 2 must not exceed 255 characters")
    private String shippingAddressLine2;

    @NotBlank(message = "Shipping city is required")
    @Size(max = 100, message = "Shipping city must not exceed 100 characters")
    private String shippingCity;

    @NotBlank(message = "Shipping state is required")
    @Size(max = 100, message = "Shipping state must not exceed 100 characters")
    private String shippingState;

    @NotBlank(message = "Shipping postal code is required")
    @Size(max = 20, message = "Shipping postal code must not exceed 20 characters")
    private String shippingPostalCode;

    @NotBlank(message = "Shipping country is required")
    @Size(max = 100, message = "Shipping country must not exceed 100 characters")
    private String shippingCountry;

    @Size(max = 20, message = "Shipping phone must not exceed 20 characters")
    private String shippingPhone;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;

    @DecimalMin(value = "0.00", message = "Tax amount must be non-negative")
    @Digits(integer = 8, fraction = 2, message = "Invalid tax amount format")
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @DecimalMin(value = "0.00", message = "Shipping amount must be non-negative")
    @Digits(integer = 8, fraction = 2, message = "Invalid shipping amount format")
    private BigDecimal shippingAmount = BigDecimal.ZERO;

    @DecimalMin(value = "0.00", message = "Discount amount must be non-negative")
    @Digits(integer = 8, fraction = 2, message = "Invalid discount amount format")
    private BigDecimal discountAmount = BigDecimal.ZERO;
}