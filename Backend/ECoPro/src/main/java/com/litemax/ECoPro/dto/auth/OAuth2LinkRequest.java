package com.litemax.ECoPro.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OAuth2LinkRequest {
    
    @NotBlank(message = "Provider is required")
    private String provider;
    
    @NotBlank(message = "Provider ID is required")
    private String providerId;
    
    @Email(message = "Valid email is required")
    private String email;
    
    private String name;
    private String profileImage;
}