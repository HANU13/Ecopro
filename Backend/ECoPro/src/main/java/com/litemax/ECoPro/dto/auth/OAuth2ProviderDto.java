package com.litemax.ECoPro.dto.auth;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OAuth2ProviderDto {
    private String provider;
    private String providerId;
    private String email;
    private String name;
    private String profileImage;
    private LocalDateTime linkedAt;
    private boolean isPrimary;
}