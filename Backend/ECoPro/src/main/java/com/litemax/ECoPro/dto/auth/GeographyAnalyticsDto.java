package com.litemax.ECoPro.dto.auth;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GeographyAnalyticsDto {
    private List<GeographicDistributionDto> countries;
    private List<GeographicDistributionDto> topRegions;
    private String primaryMarket;
    private double marketConcentration;
    private List<StatisticItemDto> geoMetrics;
}