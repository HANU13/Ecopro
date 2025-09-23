package com.litemax.ECoPro.dto.product;

import com.litemax.ECoPro.entity.product.Review.ReviewStatus;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
public class ReviewModerationRequest {
    
    @NotNull(message = "Status is required")
    private ReviewStatus status;
    
    @Size(max = 500, message = "Moderator notes must not exceed 500 characters")
    private String moderatorNotes;
}