package com.litemax.ECoPro.dto.order;

import com.litemax.ECoPro.entity.order.Shipment.ShipmentStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ShipmentResponse {
    private Long id;
    private String trackingNumber;
    private ShipmentStatus status;
    private String carrier;
    private String shippingMethod;
    private LocalDateTime shippedDate;
    private LocalDateTime estimatedDeliveryDate;
    private LocalDateTime actualDeliveryDate;
    private String notes;
    private LocalDateTime createdAt;
    private List<ShipmentItemResponse> shipmentItems;
}