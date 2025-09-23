package com.litemax.ECoPro.dto.product;

import com.litemax.ECoPro.entity.product.Review.ReviewStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReviewResponse {
    private Long id;
    private Long userId;
    private String userName; // Will be "Anonymous" if isAnonymous = true
    private Long productId;
    private String productName;
    private Long orderId;
    private Integer rating;
    private String title;
    private String reviewText;
    private ReviewStatus status;
    private Boolean isVerifiedPurchase;
    private Boolean isAnonymous;
    private Integer helpfulCount;
    private Integer unhelpfulCount;
    private String sellerResponse;
    private LocalDateTime sellerResponseAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ReviewImageResponse> images;
    private Boolean currentUserFoundHelpful; // null if not voted, true/false if voted
}