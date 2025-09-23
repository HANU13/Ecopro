package com.litemax.ECoPro.dto.auth;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class SystemStatisticsResponse {
    private Map<String, Long> databaseStatistics;
    private SystemMemoryDto memoryStatistics;
    private SystemHealthDto systemHealth;
    private List<GeographicDistributionDto> geographicDistribution;
    private String systemUptime;
    private LocalDateTime generatedAt;
}