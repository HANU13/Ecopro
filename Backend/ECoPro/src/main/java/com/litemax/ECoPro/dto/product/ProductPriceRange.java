package com.litemax.ECoPro.dto.product;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProductPriceRange {
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
}