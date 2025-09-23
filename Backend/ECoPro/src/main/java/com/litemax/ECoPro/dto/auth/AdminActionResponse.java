package com.litemax.ECoPro.dto.auth;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class AdminActionResponse {
    private boolean success;
    private String message;
    private String actionType;
    private Long targetUserId;
    private String performedBy;
    private LocalDateTime timestamp;
    private Map<String, Object> details;
    private AdminAuditLogDto auditLog;
}