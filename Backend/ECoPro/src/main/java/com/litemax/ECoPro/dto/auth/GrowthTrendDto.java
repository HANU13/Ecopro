package com.litemax.ECoPro.dto.auth;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GrowthTrendDto {
    private double currentPeriodGrowth;
    private double previousPeriodGrowth;
    private double growthRate;
    private String trend; // INCREASING, DECREASING, STABLE
    private List<DailyStatisticDto> dailyTrend;
    private String interpretation;
}