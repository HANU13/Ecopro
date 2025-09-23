package com.litemax.ECoPro.service.order;


import com.litemax.ECoPro.dto.order.*;
import com.litemax.ECoPro.entity.auth.User;
import com.litemax.ECoPro.entity.cart.Cart;
import com.litemax.ECoPro.entity.cart.CartItem;
import com.litemax.ECoPro.entity.order.Order;
import com.litemax.ECoPro.entity.order.Order.OrderStatus;
import com.litemax.ECoPro.entity.order.OrderItem;
import com.litemax.ECoPro.exception.ResourceNotFoundException;
import com.litemax.ECoPro.exception.ValidationException;
import com.litemax.ECoPro.repository.order.OrderRepository;
import com.litemax.ECoPro.repository.order.OrderItemRepository;
import com.litemax.ECoPro.service.UserService;
import com.litemax.ECoPro.service.cart.CartService;
import com.litemax.ECoPro.service.inventory.InventoryService;
import com.litemax.ECoPro.util.MapperUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartService cartService;
    private final UserService userService;
    private final InventoryService inventoryService;
    private final MapperUtil mapperUtil;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, Long userId) {
        log.info("Creating order for user ID: {} with cart ID: {}", userId, request.getCartId());

        try {
            // Get user and cart
            User user = userService.findById(userId);
            Cart cart = cartService.findById(request.getCartId());

            // Validate cart belongs to user
            if (!cart.getUser().getId().equals(userId)) {
                log.error("Unauthorized cart access: User {} trying to access cart {}", userId, request.getCartId());
                throw new ValidationException("Unauthorized cart access");
            }

            // Validate cart is not empty
            if (cart.getItems().isEmpty()) {
                log.error("Cannot create order from empty cart: {}", request.getCartId());
                throw new ValidationException("Cannot create order from empty cart");
            }

            // Check inventory for all items
            for (CartItem cartItem : cart.getItems()) {
                boolean available = inventoryService.checkAvailability(
                        cartItem.getProduct().getId(),
                        cartItem.getVariant() != null ? cartItem.getVariant().getId() : null,
                        cartItem.getQuantity()
                );

                if (!available) {
                    log.error("Insufficient inventory for product: {} variant: {} quantity: {}",
                            cartItem.getProduct().getId(),
                            cartItem.getVariant() != null ? cartItem.getVariant().getId() : null,
                            cartItem.getQuantity());
                    throw new ValidationException("Insufficient inventory for product: " + cartItem.getProduct().getName());
                }
            }

            // Calculate totals
            BigDecimal subtotal = cart.getItems().stream()
                    .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalAmount = subtotal
                    .add(request.getTaxAmount())
                    .add(request.getShippingAmount())
                    .subtract(request.getDiscountAmount());

            // Create order
            Order order = new Order();
            order.setOrderNumber(generateOrderNumber());
            order.setUser(user);
            order.setCart(cart);
            order.setStatus(OrderStatus.PLACED);
            order.setSubtotal(subtotal);
            order.setTaxAmount(request.getTaxAmount());
            order.setShippingAmount(request.getShippingAmount());
            order.setDiscountAmount(request.getDiscountAmount());
            order.setTotalAmount(totalAmount);
            order.setShippingAddressLine1(request.getShippingAddressLine1());
            order.setShippingAddressLine2(request.getShippingAddressLine2());
            order.setShippingCity(request.getShippingCity());
            order.setShippingState(request.getShippingState());
            order.setShippingPostalCode(request.getShippingPostalCode());
            order.setShippingCountry(request.getShippingCountry());
            order.setShippingPhone(request.getShippingPhone());
            order.setNotes(request.getNotes());
            order.setEstimatedDeliveryDate(LocalDateTime.now().plusDays(7)); // Default 7 days

            order = orderRepository.save(order);
            log.info("Order created with ID: {} and order number: {}", order.getId(), order.getOrderNumber());

            // Create order items
            for (CartItem cartItem : cart.getItems()) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setProduct(cartItem.getProduct());
                orderItem.setVariant(cartItem.getVariant());
                orderItem.setQuantity(cartItem.getQuantity());
                orderItem.setUnitPrice(cartItem.getUnitPrice());
                orderItem.setTotalPrice(cartItem.getUnitPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
                orderItem.setProductName(cartItem.getProduct().getName());
                orderItem.setVariantName(cartItem.getVariant() != null ?
                        cartItem.getVariant().getName() : null);

                orderItemRepository.save(orderItem);
                log.debug("Order item created for product: {} quantity: {}",
                        cartItem.getProduct().getName(), cartItem.getQuantity());

                // Reserve inventory
                inventoryService.reserveInventory(
                        cartItem.getProduct().getId(),
                        cartItem.getVariant() != null ? cartItem.getVariant().getId() : null,
                        cartItem.getQuantity()
                );
            }

            // Clear cart after successful order creation
            cartService.clearCart(user.getEmail());
            log.info("Cart cleared after order creation: {}", request.getCartId());

            // Convert to response
            OrderResponse response = mapperUtil.mapToOrderResponse(order);
            log.info("Order created successfully: {}", order.getOrderNumber());
            return response;

        } catch (Exception e) {
            log.error("Error creating order for user {}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, Long userId) {
        log.info("Fetching order ID: {} for user ID: {}", orderId, userId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("Order not found with ID: {}", orderId);
                    return new ResourceNotFoundException("Order not found with ID: " + orderId);
                });

        // Check if user has access to this order
        if (!order.getUser().getId().equals(userId)) {
            log.error("Unauthorized order access: User {} trying to access order {}", userId, orderId);
            throw new ValidationException("Unauthorized access to order");
        }

        log.debug("Order retrieved successfully: {}", order.getOrderNumber());
        return mapperUtil.mapToOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber, Long userId) {
        log.info("Fetching order by number: {} for user ID: {}", orderNumber, userId);

        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> {
                    log.error("Order not found with number: {}", orderNumber);
                    return new ResourceNotFoundException("Order not found with number: " + orderNumber);
                });

        // Check if user has access to this order
        if (!order.getUser().getId().equals(userId)) {
            log.error("Unauthorized order access: User {} trying to access order {}", userId, orderNumber);
            throw new ValidationException("Unauthorized access to order");
        }

        log.debug("Order retrieved successfully by number: {}", orderNumber);
        return mapperUtil.mapToOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrders(Long userId, Pageable pageable) {
        log.info("Fetching orders for user ID: {}, page: {}, size: {}",
                userId, pageable.getPageNumber(), pageable.getPageSize());

        User user = userService.findById(userId);
        Page<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        log.debug("Found {} orders for user: {}", orders.getTotalElements(), userId);
        return orders.map(mapperUtil::mapToOrderResponse);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrdersByStatus(Long userId, OrderStatus status, Pageable pageable) {
        log.info("Fetching orders for user ID: {} with status: {}", userId, status);

        User user = userService.findById(userId);
        Page<Order> orders = orderRepository.findByUserAndStatus(user, status, pageable);

        log.debug("Found {} orders with status {} for user: {}", orders.getTotalElements(), status, userId);
        return orders.map(mapperUtil::mapToOrderResponse);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request, Long adminUserId) {
        log.info("Updating order status: Order ID: {}, New Status: {}, Admin ID: {}",
                orderId, request.getStatus(), adminUserId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("Order not found with ID: {}", orderId);
                    return new ResourceNotFoundException("Order not found with ID: " + orderId);
                });

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(request.getStatus());

        // Update delivery date if status is DELIVERED
        if (request.getStatus() == OrderStatus.DELIVERED) {
            order.setActualDeliveryDate(LocalDateTime.now());
        }

        // Add notes if provided
        if (request.getAdminNotes() != null && !request.getAdminNotes().trim().isEmpty()) {
            String existingNotes = order.getNotes() != null ? order.getNotes() : "";
            order.setNotes(existingNotes + "\n[" + LocalDateTime.now() + "] " + request.getAdminNotes());
        }

        order = orderRepository.save(order);

        log.info("Order status updated successfully: Order: {}, From: {} To: {}",
                order.getOrderNumber(), oldStatus, request.getStatus());

        // TODO: Send notification to customer about status change

        return mapperUtil.mapToOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        log.info("Fetching all orders, page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<Order> orders = orderRepository.findAll(pageable);
        log.debug("Found {} total orders", orders.getTotalElements());

        return orders.map(mapperUtil::mapToOrderResponse);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        log.info("Fetching orders with status: {}", status);

        Page<Order> orders = orderRepository.findByStatus(status, pageable);
        log.debug("Found {} orders with status: {}", orders.getTotalElements(), status);

        return orders.map(mapperUtil::mapToOrderResponse);
    }

    @Transactional
    public void cancelOrder(Long orderId, Long userId, String reason) {
        log.info("Cancelling order ID: {} by user ID: {}, reason: {}", orderId, userId, reason);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("Order not found with ID: {}", orderId);
                    return new ResourceNotFoundException("Order not found with ID: " + orderId);
                });

        // Check if user has access to this order
        if (!order.getUser().getId().equals(userId)) {
            log.error("Unauthorized order cancellation: User {} trying to cancel order {}", userId, orderId);
            throw new ValidationException("Unauthorized access to order");
        }

        // Check if order can be cancelled
        if (!canBeCancelled(order.getStatus())) {
            log.error("Order cannot be cancelled in current status: {} for order: {}",
                    order.getStatus(), order.getOrderNumber());
            throw new ValidationException("Order cannot be cancelled in current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);

        // Add cancellation note
        String cancellationNote = "[" + LocalDateTime.now() + "] Order cancelled by user. Reason: " +
                (reason != null ? reason : "Not specified");
        String existingNotes = order.getNotes() != null ? order.getNotes() : "";
        order.setNotes(existingNotes + "\n" + cancellationNote);

        orderRepository.save(order);

        // Release reserved inventory
        for (OrderItem item : order.getOrderItems()) {
            inventoryService.releaseInventory(
                    item.getProduct().getId(),
                    item.getVariant() != null ? item.getVariant().getId() : null,
                    item.getQuantity()
            );
        }

        log.info("Order cancelled successfully: {}", order.getOrderNumber());

        // TODO: Process refund if payment was made
        // TODO: Send cancellation notification to customer
    }

    private boolean canBeCancelled(OrderStatus status) {
        return status == OrderStatus.PLACED || status == OrderStatus.CONFIRMED;
    }

    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // Admin/Analytics methods
    @Transactional(readOnly = true)
    public Long getOrderCountByStatus(OrderStatus status) {
        log.debug("Getting order count for status: {}", status);
        return orderRepository.countByStatus(status);
    }

    @Transactional(readOnly = true)
    public Double getTotalRevenue() {
        log.debug("Calculating total revenue");
        List<OrderStatus> revenueStatuses = List.of(
                OrderStatus.CONFIRMED, OrderStatus.PROCESSING,
                OrderStatus.SHIPPED, OrderStatus.DELIVERED
        );
        return orderRepository.getTotalRevenueByStatus(revenueStatuses);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersBetweenDates(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Fetching orders between {} and {}", startDate, endDate);

        List<Order> orders = orderRepository.findByCreatedAtBetween(startDate, endDate);
        log.debug("Found {} orders between specified dates", orders.size());

        return orders.stream()
                .map(mapperUtil::mapToOrderResponse)
                .collect(Collectors.toList());
    }
}
