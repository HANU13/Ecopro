package com.litemax.ECoPro.dto.auth;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StateDistributionDto {
    private String state;
    private String stateCode;
    private long userCount;
    private double percentage;
}