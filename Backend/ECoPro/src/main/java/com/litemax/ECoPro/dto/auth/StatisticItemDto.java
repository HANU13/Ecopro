package com.litemax.ECoPro.dto.auth;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatisticItemDto {
    private String label;
    private long value;
    private String unit;
    private String trend; // UP, DOWN, STABLE
    private double changePercentage;
    private String icon;
    private String color;
}