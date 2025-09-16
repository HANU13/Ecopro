package com.litemax.ECoPro.dto.auth;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String message;
    private String accessToken;
    private String refreshToken;
    private UserResponse user;
}