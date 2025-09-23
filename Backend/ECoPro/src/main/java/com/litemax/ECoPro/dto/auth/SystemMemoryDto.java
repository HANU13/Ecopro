package com.litemax.ECoPro.dto.auth;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SystemMemoryDto {
    private long totalMemory;
    private long freeMemory;
    private long usedMemory;
    private long maxMemory;
    private double memoryUsagePercentage;
    private String status; // HEALTHY, WARNING, CRITICAL
}