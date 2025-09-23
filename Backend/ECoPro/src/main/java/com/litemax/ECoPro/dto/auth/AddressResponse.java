package com.litemax.ECoPro.dto.auth;

import java.time.LocalDateTime;

import com.litemax.ECoPro.entity.auth.Address;

import lombok.Builder;
import lombok.Data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AddressResponse {
    private Long id;
    private String fullName;
    private String phone;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    private Address.AddressType type;
    private boolean isDefault;
    private String formattedAddress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Additional fields for admin view
    private Long userId;
    private String userEmail;
    private boolean isActive;
}