package com.litemax.ECoPro.dto.product;

import lombok.Data;

import jakarta.validation.constraints.*;

@Data
public class ReviewUpdateRequest {
    
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    private Integer rating;
    
    @Size(max = 100, message = "Title must not exceed 100 characters")
    private String title;
    
    @Size(min = 10, max = 2000, message = "Review text must be between 10 and 2000 characters")
    private String reviewText;
    
    private Boolean isAnonymous;
}