package com.litemax.ECoPro.dto.auth;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ActivityAnalyticsDto {
    private long totalSessions;
    private double averageSessionDuration;
    private long uniqueActiveUsers;
    private double bounceRate;
    private Map<String, Long> activitiesByType;
    private List<DailyStatisticDto> dailyActivities;
    private List<StatisticItemDto> engagementMetrics;
}