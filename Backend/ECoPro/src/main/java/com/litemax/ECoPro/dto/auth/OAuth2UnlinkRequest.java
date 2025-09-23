package com.litemax.ECoPro.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OAuth2UnlinkRequest {
    
    @NotBlank(message = "Provider is required")
    private String provider;
    
    private String reason;
    private String password; // Required if it's the last auth method
}