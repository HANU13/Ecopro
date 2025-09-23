package com.litemax.ECoPro.dto.product;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CategoryStatisticsResponse {
    private long totalCategories;
    private long activeCategories;
    private long rootCategories;
    private long featuredCategories;
    private List<CategoryProductCount> topCategoriesByProducts;
    private int maxDepth;

    @Data
    @Builder
    public static class CategoryProductCount {
        private Long categoryId;
        private String categoryName;
        private long productCount;
    }
}