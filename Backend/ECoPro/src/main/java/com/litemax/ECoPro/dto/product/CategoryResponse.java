package com.litemax.ECoPro.dto.product;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CategoryResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String imageUrl;
    private String iconUrl;
    private boolean active;
    private boolean featured;
    private Integer sortOrder;
    private String metaTitle;
    private String metaDescription;
    private String metaKeywords;
    private Long productCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Relationships
    private CategoryResponse parent;
    private List<CategoryResponse> children;

    // Additional fields
    private boolean hasChildren;
    private String fullPath;
    private int depth;
    private List<CategoryResponse> breadcrumbs;
}