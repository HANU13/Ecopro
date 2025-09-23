package com.litemax.ECoPro.dto.order;

import com.litemax.ECoPro.entity.order.Order;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateOrderStatusRequest {

    @NotNull(message = "Order status is required")
    private Order.OrderStatus status;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String adminNotes;
    private LocalDateTime estimatedDeliveryDate;
    private LocalDateTime actualDeliveryDate;
}
