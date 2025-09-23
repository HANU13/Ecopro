package com.litemax.ECoPro.dto.product;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class SellerDashboardResponse {
    private long totalProducts;
    private long activeProducts;
    private long draftProducts;
    private long outOfStockProducts;
    private long lowStockProducts;
    private BigDecimal totalRevenue;
    private long totalViews;
    private BigDecimal averageRating;
    private List<ProductListResponse> topProducts;
    private List<CategoryProductCount> productsByCategory;
    private RecentActivitySummary recentActivity;

    @Data
    @Builder
    public static class CategoryProductCount {
        private String categoryName;
        private long productCount;
    }

    @Data
    @Builder
    public static class RecentActivitySummary {
        private long productsAddedThisWeek;
        private long ordersThisWeek;
        private long viewsThisWeek;
    }
}