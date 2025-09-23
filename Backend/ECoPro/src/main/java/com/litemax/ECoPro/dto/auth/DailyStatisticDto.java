package com.litemax.ECoPro.dto.auth;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class DailyStatisticDto {
    private LocalDate date;
    private long count;
    private String label;
    private double percentage;
}