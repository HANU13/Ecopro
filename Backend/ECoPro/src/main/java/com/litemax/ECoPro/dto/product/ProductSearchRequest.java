package com.litemax.ECoPro.dto.product;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductSearchRequest {
    
    private String keyword;
    private List<Long> categoryIds;
    private List<Long> brandIds;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private BigDecimal minRating;
    private Boolean featured;
    private Boolean inStock;
    private String sortBy = "createdAt"; // createdAt, price, rating, name, popularity
    private String sortDirection = "desc"; // asc, desc
    private Integer page = 0;
    private Integer size = 20;
    
    // Advanced filters
    private List<String> tags;
    private String vendor;
    private Boolean digital;
    private String status = "ACTIVE";
}