package com.litemax.ECoPro.dto.product;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ProductSearchResponse {
    private List<ProductListResponse> products;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int size;
    private boolean hasNext;
    private boolean hasPrevious;
    
    // Filter aggregations
    private Map<String, Long> categoryFacets;
    private Map<String, Long> brandFacets;
    private Map<String, Long> priceFacets;
    private Map<String, Long> ratingFacets;
    private ProductPriceRange priceRange;
}