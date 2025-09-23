package com.litemax.ECoPro.dto.auth;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class BulkOperationItemResult {
    private Long userId;
    private String userEmail;
    private boolean success;
    private String message;
    private String errorCode;
    private Map<String, Object> details;
    private LocalDateTime processedAt;
}