package com.litemax.ECoPro.dto.product;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class SellerResponseRequest {
    
    @NotBlank(message = "Response is required")
    @Size(min = 10, max = 1000, message = "Response must be between 10 and 1000 characters")
    private String response;
}