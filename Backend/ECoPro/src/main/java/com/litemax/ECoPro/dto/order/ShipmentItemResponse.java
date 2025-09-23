package com.litemax.ECoPro.dto.order;

import lombok.Data;

@Data
public class ShipmentItemResponse {
    private Long id;
    private Long orderItemId;
    private Integer quantity;
    private String notes;
}