package com.litemax.ECoPro.dto.auth;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GeographicDistributionDto {
    private String country;
    private String countryCode;
    private long userCount;
    private double percentage;
    private List<StateDistributionDto> states;
}