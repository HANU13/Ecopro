package com.litemax.ECoPro.dto.auth;


import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class AdminDashboardResponse {
    private long totalUsers;
    private long activeUsers;
    private long inactiveUsers;
    private long bannedUsers;
    private long lockedUsers;
    private long recentRegistrations;
    private long recentLogins;
    private long todayRegistrations;
    private double userGrowthRate;
    private Map<String, Long> roleDistribution;
    private List<RecentActivityDto> recentActivities;
    private SystemHealthDto systemHealth;
    private List<StatisticItemDto> topStatistics;
    private LocalDateTime generatedAt;
}

