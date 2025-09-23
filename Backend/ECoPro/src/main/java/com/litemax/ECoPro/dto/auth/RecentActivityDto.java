package com.litemax.ECoPro.dto.auth;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RecentActivityDto {
    private Long id;
    private String action;
    private String description;
    private String userEmail;
    private String ipAddress;
    private String severity;
    private LocalDateTime timestamp;
}