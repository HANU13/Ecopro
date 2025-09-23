package com.litemax.ECoPro.dto.product;

import lombok.Data;

@Data
public class ReviewImageResponse {
    private Long id;
    private String imageUrl;
    private String altText;
    private Integer displayOrder;
}