package com.litemax.ECoPro.dto.product;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductRatingResponse {
    private Long productId;
    private BigDecimal averageRating;
    private Integer totalReviews;
    private Integer rating1Count;
    private Integer rating2Count;
    private Integer rating3Count;
    private Integer rating4Count;
    private Integer rating5Count;
    private BigDecimal rating1Percentage;
    private BigDecimal rating2Percentage;
    private BigDecimal rating3Percentage;
    private BigDecimal rating4Percentage;
    private BigDecimal rating5Percentage;
    private LocalDateTime updatedAt;
}
