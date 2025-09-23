package com.litemax.ECoPro.service.order;

import com.litemax.ECoPro.dto.order.ShipmentResponse;
import com.litemax.ECoPro.entity.order.Order;
import com.litemax.ECoPro.entity.order.OrderItem;
import com.litemax.ECoPro.entity.order.Shipment;
import com.litemax.ECoPro.entity.order.Shipment.ShipmentStatus;
import com.litemax.ECoPro.entity.order.ShipmentItem;
import com.litemax.ECoPro.exception.ResourceNotFoundException;
import com.litemax.ECoPro.exception.ValidationException;
import com.litemax.ECoPro.repository.order.ShipmentRepository;
import com.litemax.ECoPro.repository.order.OrderRepository;
import com.litemax.ECoPro.util.MapperUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShipmentService {
    
    private final ShipmentRepository shipmentRepository;
    private final OrderRepository orderRepository;
    private final MapperUtil mapperUtil;
    
    @Transactional
    public ShipmentResponse createShipment(Long orderId, String carrier, String shippingMethod, Long adminUserId) {
        log.info("Creating shipment for order ID: {} with carrier: {} by admin: {}", 
                orderId, carrier, adminUserId);
        
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> {
                log.error("Order not found with ID: {}", orderId);
                return new ResourceNotFoundException("Order not found with ID: " + orderId);
            });
        
        // Validate order status
        if (order.getStatus() != Order.OrderStatus.CONFIRMED && order.getStatus() != Order.OrderStatus.PROCESSING) {
            log.error("Invalid order status for shipment creation: {} for order: {}", 
                     order.getStatus(), order.getOrderNumber());
            throw new ValidationException("Order is not ready for shipment");
        }
        
        // Create shipment
        Shipment shipment = new Shipment();
        shipment.setTrackingNumber(generateTrackingNumber());
        shipment.setOrder(order);
        shipment.setStatus(ShipmentStatus.PREPARING);
        shipment.setCarrier(carrier);
        shipment.setShippingMethod(shippingMethod);
        shipment.setEstimatedDeliveryDate(LocalDateTime.now().plusDays(getEstimatedDeliveryDays(shippingMethod)));
        
        shipment = shipmentRepository.save(shipment);
        log.info("Shipment created with tracking number: {} for order: {}", 
                shipment.getTrackingNumber(), order.getOrderNumber());
        
        // Create shipment items for all order items
        for (OrderItem orderItem : order.getOrderItems()) {
            ShipmentItem shipmentItem = new ShipmentItem();
            shipmentItem.setShipment(shipment);
            shipmentItem.setOrderItem(orderItem);
            shipmentItem.setQuantity(orderItem.getQuantity());
            
            shipment.getShipmentItems().add(shipmentItem);
            log.debug("Shipment item created for order item: {} quantity: {}", 
                     orderItem.getId(), orderItem.getQuantity());
        }
        
        // Update order status
        order.setStatus(Order.OrderStatus.PROCESSING);
        orderRepository.save(order);
        
        return mapperUtil.mapToShipmentResponse(shipment);
    }
    
    @Transactional
    public ShipmentResponse updateShipmentStatus(Long shipmentId, ShipmentStatus status, String notes, Long adminUserId) {
        log.info("Updating shipment status: Shipment ID: {}, New Status: {}, Admin: {}", 
                shipmentId, status, adminUserId);
        
        Shipment shipment = shipmentRepository.findById(shipmentId)
            .orElseThrow(() -> {
                log.error("Shipment not found with ID: {}", shipmentId);
                return new ResourceNotFoundException("Shipment not found with ID: " + shipmentId);
            });
        
        ShipmentStatus oldStatus = shipment.getStatus();
        shipment.setStatus(status);
        
        // Update dates based on status
        switch (status) {
            case SHIPPED:
                shipment.setShippedDate(LocalDateTime.now());
                // Update order status
                shipment.getOrder().setStatus(Order.OrderStatus.SHIPPED);
                break;
            case DELIVERED:
                shipment.setActualDeliveryDate(LocalDateTime.now());
                // Update order status and delivery date
                Order order = shipment.getOrder();
                order.setStatus(Order.OrderStatus.DELIVERED);
                order.setActualDeliveryDate(LocalDateTime.now());
                break;
        }
        
        // Add notes if provided
        if (notes != null && !notes.trim().isEmpty()) {
            String existingNotes = shipment.getNotes() != null ? shipment.getNotes() : "";
            shipment.setNotes(existingNotes + "\n[" + LocalDateTime.now() + "] " + notes);
        }
        
        shipment = shipmentRepository.save(shipment);
        orderRepository.save(shipment.getOrder());
        
        log.info("Shipment status updated: Tracking: {}, From: {} To: {}", 
                shipment.getTrackingNumber(), oldStatus, status);
        
        // TODO: Send tracking update notification to customer
        
        return mapperUtil.mapToShipmentResponse(shipment);
    }
    
    @Transactional(readOnly = true)
    public ShipmentResponse getShipmentByTrackingNumber(String trackingNumber) {
        log.info("Fetching shipment by tracking number: {}", trackingNumber);
        
        Shipment shipment = shipmentRepository.findByTrackingNumber(trackingNumber)
            .orElseThrow(() -> {
                log.error("Shipment not found with tracking number: {}", trackingNumber);
                return new ResourceNotFoundException("Shipment not found with tracking number: " + trackingNumber);
            });
        
        log.debug("Shipment retrieved: {}", trackingNumber);
        return mapperUtil.mapToShipmentResponse(shipment);
    }
    
    @Transactional(readOnly = true)
    public List<ShipmentResponse> getOrderShipments(Long orderId, Long userId) {
        log.info("Fetching shipments for order ID: {} by user: {}", orderId, userId);
        
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> {
                log.error("Order not found with ID: {}", orderId);
                return new ResourceNotFoundException("Order not found with ID: " + orderId);
            });
        
        // Validate user has access to this order
        if (!order.getUser().getId().equals(userId)) {
            log.error("Unauthorized order access: User {} for order {}", userId, orderId);
            throw new ValidationException("Unauthorized access to order");
        }
        
        List<Shipment> shipments = shipmentRepository.findByOrderId(orderId);
        log.debug("Found {} shipments for order: {}", shipments.size(), orderId);
        
        return shipments.stream()
            .map(mapperUtil::mapToShipmentResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public Page<ShipmentResponse> getShipmentsByStatus(ShipmentStatus status, Pageable pageable) {
        log.info("Fetching shipments with status: {}", status);
        
        Page<Shipment> shipments = shipmentRepository.findByStatus(status, pageable);
        log.debug("Found {} shipments with status: {}", shipments.getTotalElements(), status);
        
        return shipments.map(mapperUtil::mapToShipmentResponse);
    }
    
    private String generateTrackingNumber() {
        return "TRK-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
    
    private int getEstimatedDeliveryDays(String shippingMethod) {
        if (shippingMethod == null) return 7;
        
        switch (shippingMethod.toLowerCase()) {
            case "express":
            case "overnight":
                return 1;
            case "priority":
            case "2-day":
                return 2;
            case "expedited":
                return 3;
            default:
                return 7; // Standard shipping
        }
    }
}
