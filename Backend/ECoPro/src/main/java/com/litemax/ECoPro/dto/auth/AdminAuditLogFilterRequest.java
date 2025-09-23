package com.litemax.ECoPro.dto.auth;


import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminAuditLogFilterRequest {
    private String action;
    private Long performedBy;
    private Long targetUserId;
    private String severity;
    private List<String> actions;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime dateFrom;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime dateTo;
    
    private String searchTerm;
    private String ipAddress;
}