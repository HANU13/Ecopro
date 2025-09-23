package com.litemax.ECoPro.dto.auth;


import com.litemax.ECoPro.entity.auth.Address;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddressUpdateRequest {

    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    private String phone;

    @Size(max = 255, message = "Address line 1 cannot exceed 255 characters")
    private String addressLine1;

    @Size(max = 255, message = "Address line 2 cannot exceed 255 characters")
    private String addressLine2;

    @Size(max = 100, message = "City cannot exceed 100 characters")
    private String city;

    @Size(max = 100, message = "State cannot exceed 100 characters")
    private String state;

    @Size(max = 20, message = "Zip code cannot exceed 20 characters")
    private String zipCode;

    @Size(max = 100, message = "Country cannot exceed 100 characters")
    private String country;

    private Address.AddressType type;

    private Boolean isDefault;
}