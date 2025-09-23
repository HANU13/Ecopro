package com.litemax.ECoPro.dto.product;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductAttributeResponse {
    private Long id;
    private Long attributeId;
    private String attributeName;
    private String attributeCode;
    private String value;
    private Integer sortOrder;
}