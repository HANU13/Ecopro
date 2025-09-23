package com.litemax.ECoPro.dto.auth;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class AdminAnalyticsResponse {
    private String period;
    private String category;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private UserAnalyticsDto userAnalytics;
    private RegistrationAnalyticsDto registrationAnalytics;
    private ActivityAnalyticsDto activityAnalytics;
    private GeographyAnalyticsDto geographyAnalytics;
    private LocalDateTime generatedAt;
}