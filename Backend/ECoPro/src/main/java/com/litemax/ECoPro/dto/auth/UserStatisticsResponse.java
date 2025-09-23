package com.litemax.ECoPro.dto.auth;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.litemax.ECoPro.entity.auth.User;

@Data
@Builder
public class UserStatisticsResponse {
    private String period;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private long totalUsers;
    private long newRegistrations;
    private long activeUsers;
    private Map<User.UserStatus, Long> statusDistribution;
    private Map<String, Long> roleDistribution;
    private List<DailyStatisticDto> dailyRegistrations;
    private GrowthTrendDto growthTrend;
    private LocalDateTime generatedAt;
}