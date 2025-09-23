package com.litemax.ECoPro.dto.product;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductAttributeRequest {

    @NotNull(message = "Attribute ID is required")
    private Long attributeId;

    private String value;
    private Integer sortOrder = 0;
}