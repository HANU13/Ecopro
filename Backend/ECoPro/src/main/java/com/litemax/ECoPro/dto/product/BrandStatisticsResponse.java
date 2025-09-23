package com.litemax.ECoPro.dto.product;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BrandStatisticsResponse {
    private long totalBrands;
    private long activeBrands;
    private long featuredBrands;
    private List<BrandProductCount> topBrandsByProducts;
    private List<String> topCountries;

    @Data
    @Builder
    public static class BrandProductCount {
        private Long brandId;
        private String brandName;
        private long productCount;
    }
}