package com.litemax.ECoPro.dto.auth;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class AdminAuditLogDto {
    private Long id;
    private String action;
    private String description;
    private String reason;
    private String performedBy;
    private Long targetUserId;
    private String targetUserEmail;
    private String ipAddress;
    private String userAgent;
    private Map<String, Object> metadata;
    private String severity; // INFO, WARNING, ERROR, CRITICAL
    private LocalDateTime timestamp;
}