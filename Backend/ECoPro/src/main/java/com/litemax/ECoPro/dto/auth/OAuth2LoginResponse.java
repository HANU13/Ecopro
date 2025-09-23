package com.litemax.ECoPro.dto.auth;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OAuth2LoginResponse {
    private Long userId;
    private String email;
    private boolean hasPassword;
    private List<OAuth2ProviderDto> linkedProviders;
    private int totalProviders;
    private boolean canUnlinkAll;
    private String message;
}