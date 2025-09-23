package com.litemax.ECoPro.dto.auth;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserActivitySummaryDto {
    private long totalLogins;
    private LocalDateTime firstLogin;
    private LocalDateTime lastLogin;
    private long sessionsThisMonth;
    private long averageSessionDuration;
    private String mostActiveDay;
    private String mostActiveHour;
    private long totalOrders;
    private double totalSpent;
    private String accountAge;
    private boolean isActiveUser;
}