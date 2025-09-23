package com.litemax.ECoPro.dto.auth;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class UserAnalyticsDto {
    private long totalUsers;
    private long activeUsers;
    private long newUsers;
    private double retentionRate;
    private double churnRate;
    private Map<String, Long> usersByStatus;
    private List<DailyStatisticDto> dailyActiveUsers;
    private List<StatisticItemDto> topMetrics;
}