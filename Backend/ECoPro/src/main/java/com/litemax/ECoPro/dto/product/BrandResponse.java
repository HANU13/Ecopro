package com.litemax.ECoPro.dto.product;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BrandResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String logoUrl;
    private String bannerUrl;
    private String websiteUrl;
    private boolean active;
    private boolean featured;
    private Integer sortOrder;
    private String metaTitle;
    private String metaDescription;
    private String metaKeywords;
    private Long productCount;
    private String originCountry;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}