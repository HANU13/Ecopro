package com.litemax.ECoPro.dto.auth;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class RegistrationAnalyticsDto {
    private long totalRegistrations;
    private double averageDaily;
    private String peakRegistrationDay;
    private String peakRegistrationHour;
    private Map<String, Long> registrationsBySource;
    private List<DailyStatisticDto> dailyRegistrations;
    private GrowthTrendDto registrationTrend;
}