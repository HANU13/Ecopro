package com.litemax.ECoPro.controller.order;

import com.litemax.ECoPro.dto.order.ShipmentResponse;
import com.litemax.ECoPro.entity.order.Shipment.ShipmentStatus;
import com.litemax.ECoPro.service.order.ShipmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
@Slf4j
public class ShipmentController {

    private final ShipmentService shipmentService;

    // Public tracking API (no authentication required)
    @GetMapping("/track/{trackingNumber}")
    public ResponseEntity<ShipmentResponse> trackShipment(@PathVariable String trackingNumber) {
        log.info("Public tracking request for: {}", trackingNumber);

        ShipmentResponse shipment = shipmentService.getShipmentByTrackingNumber(trackingNumber);
        return ResponseEntity.ok(shipment);
    }

    // Customer APIs
    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<ShipmentResponse>> getOrderShipments(@PathVariable Long orderId,
                                                                    Authentication authentication) {
        log.info("Fetching shipments for order: {} by user: {}", orderId, authentication.getName());

        Long userId = getUserIdFromAuthentication(authentication);
        List<ShipmentResponse> shipments = shipmentService.getOrderShipments(orderId, userId);

        log.debug("Found {} shipments for order: {}", shipments.size(), orderId);
        return ResponseEntity.ok(shipments);
    }

    // Admin APIs
    @PostMapping("/admin/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShipmentResponse> createShipment(@RequestParam Long orderId,
                                                           @RequestParam @NotBlank String carrier,
                                                           @RequestParam @NotBlank String shippingMethod,
                                                           Authentication authentication) {
        log.info("Creating shipment for order: {} with carrier: {} by admin: {}",
                orderId, carrier, authentication.getName());

        Long adminUserId = getUserIdFromAuthentication(authentication);
        ShipmentResponse shipment = shipmentService.createShipment(orderId, carrier, shippingMethod, adminUserId);

        log.info("Shipment created successfully with tracking: {}", shipment.getTrackingNumber());
        return ResponseEntity.status(HttpStatus.CREATED).body(shipment);
    }

    @PutMapping("/admin/{shipmentId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShipmentResponse> updateShipmentStatus(@PathVariable Long shipmentId,
                                                                 @RequestParam ShipmentStatus status,
                                                                 @RequestParam(required = false) String notes,
                                                                 Authentication authentication) {
        log.info("Updating shipment status: ID: {}, Status: {} by admin: {}",
                shipmentId, status, authentication.getName());

        Long adminUserId = getUserIdFromAuthentication(authentication);
        ShipmentResponse shipment = shipmentService.updateShipmentStatus(shipmentId, status, notes, adminUserId);

        log.info("Shipment status updated successfully: {}", shipment.getTrackingNumber());
        return ResponseEntity.ok(shipment);
    }

    @GetMapping("/admin/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ShipmentResponse>> getShipmentsByStatus(@PathVariable ShipmentStatus status,
                                                                       @RequestParam(defaultValue = "0") int page,
                                                                       @RequestParam(defaultValue = "20") int size) {
        log.info("Admin fetching shipments with status: {}", status);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ShipmentResponse> shipments = shipmentService.getShipmentsByStatus(status, pageable);

        return ResponseEntity.ok(shipments);
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        return Long.parseLong(authentication.getName()); // Adjust as needed
    }
}
