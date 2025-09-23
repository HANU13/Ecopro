package com.litemax.ECoPro.dto.auth;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class AddressValidationResponse {
    private boolean isValid;
    private boolean isVerified;
    private String confidence; // HIGH, MEDIUM, LOW
    private List<String> suggestions;
    private Map<String, String> corrections;
    private String validatedAddress;
    private Map<String, Object> metadata;
}