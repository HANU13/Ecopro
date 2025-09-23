package com.litemax.ECoPro.dto.auth;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class SystemHealthDto {
    private String overallStatus; // HEALTHY, WARNING, UNHEALTHY
    private Map<String, String> healthChecks;
    private LocalDateTime lastChecked;
    private String version;
    private long uptime;
}