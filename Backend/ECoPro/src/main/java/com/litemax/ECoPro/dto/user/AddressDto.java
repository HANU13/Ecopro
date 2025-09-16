package com.litemax.ECoPro.dto.user;
import lombok.*;

@Data
public class AddressDto {
    private String street;
    private String city;
    private String state;
    private String country;
    private String zipCode;
}