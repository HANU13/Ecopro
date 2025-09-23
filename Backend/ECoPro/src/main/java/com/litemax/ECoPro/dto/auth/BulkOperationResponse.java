package com.litemax.ECoPro.dto.auth;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BulkOperationResponse {
    private String operation;
    private int totalRequested;
    private int successCount;
    private int failedCount;
    private List<BulkOperationItemResult> results;
    private String performedBy;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long durationMs;
}