package com.litemax.ECoPro.dto.auth;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

import com.litemax.ECoPro.entity.auth.Address;

@Data
@Builder
public class AddressDto {
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
}