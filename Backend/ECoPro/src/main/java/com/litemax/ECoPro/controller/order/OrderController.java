package com.litemax.ECoPro.controller.order;

import com.litemax.ECoPro.dto.order.*;
import com.litemax.ECoPro.entity.order.Order.OrderStatus;
import com.litemax.ECoPro.service.order.OrderService;
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

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    // Customer APIs
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request,
                                                     Authentication authentication) {
        log.info("Creating order for user: {}", authentication.getName());

        Long userId = getUserIdFromAuthentication(authentication);
        OrderResponse order = orderService.createOrder(request, userId);

        log.info("Order created successfully with number: {}", order.getOrderNumber());
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id,
                                                      Authentication authentication) {
        log.info("Fetching order ID: {} for user: {}", id, authentication.getName());

        Long userId = getUserIdFromAuthentication(authentication);
        OrderResponse order = orderService.getOrderById(id, userId);

        return ResponseEntity.ok(order);
    }

    @GetMapping("/number/{orderNumber}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> getOrderByNumber(@PathVariable String orderNumber,
                                                          Authentication authentication) {
        log.info("Fetching order by number: {} for user: {}", orderNumber, authentication.getName());

        Long userId = getUserIdFromAuthentication(authentication);
        OrderResponse order = orderService.getOrderByNumber(orderNumber, userId);

        return ResponseEntity.ok(order);
    }

    @GetMapping("/my-orders")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Page<OrderResponse>> getMyOrders(@RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "10") int size,
                                                           @RequestParam(defaultValue = "createdAt") String sortBy,
                                                           @RequestParam(defaultValue = "desc") String sortDir,
                                                           Authentication authentication) {
        log.info("Fetching orders for user: {}, page: {}, size: {}", authentication.getName(), page, size);

        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Long userId = getUserIdFromAuthentication(authentication);
        Page<OrderResponse> orders = orderService.getUserOrders(userId, pageable);

        log.debug("Retrieved {} orders for user: {}", orders.getTotalElements(), authentication.getName());
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/my-orders/status/{status}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Page<OrderResponse>> getMyOrdersByStatus(@PathVariable OrderStatus status,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "10") int size,
                                                                   Authentication authentication) {
        log.info("Fetching orders with status {} for user: {}", status, authentication.getName());

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Long userId = getUserIdFromAuthentication(authentication);
        Page<OrderResponse> orders = orderService.getUserOrdersByStatus(userId, status, pageable);

        return ResponseEntity.ok(orders);
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long id,
                                            @RequestParam(required = false) String reason,
                                            Authentication authentication) {
        log.info("Cancelling order ID: {} by user: {}", id, authentication.getName());

        Long userId = getUserIdFromAuthentication(authentication);
        orderService.cancelOrder(id, userId, reason);

        log.info("Order cancelled successfully: {}", id);
        return ResponseEntity.ok().build();
    }

    // Admin/Seller APIs
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<OrderResponse>> getAllOrders(@RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size,
                                                            @RequestParam(defaultValue = "createdAt") String sortBy,
                                                            @RequestParam(defaultValue = "desc") String sortDir,
                                                            Authentication authentication) {
        log.info("Admin fetching all orders: {}, page: {}, size: {}", authentication.getName(), page, size);

        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<OrderResponse> orders = orderService.getAllOrders(pageable);

        log.debug("Admin retrieved {} orders", orders.getTotalElements());
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/admin/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<OrderResponse>> getOrdersByStatus(@PathVariable OrderStatus status,
                                                                 @RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "20") int size,
                                                                 Authentication authentication) {
        log.info("Admin fetching orders with status: {}", status);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<OrderResponse> orders = orderService.getOrdersByStatus(status, pageable);

        return ResponseEntity.ok(orders);
    }

    @PutMapping("/admin/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> updateOrderStatus(@PathVariable Long id,
                                                           @Valid @RequestBody UpdateOrderStatusRequest request,
                                                           Authentication authentication) {
        log.info("Admin updating order status: Order ID: {}, New Status: {}", id, request.getStatus());

        Long adminUserId = getUserIdFromAuthentication(authentication);
        OrderResponse order = orderService.updateOrderStatus(id, request, adminUserId);

        log.info("Order status updated successfully: {}", order.getOrderNumber());
        return ResponseEntity.ok(order);
    }

    // Analytics APIs for Admin
    @GetMapping("/admin/analytics/count-by-status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long> getOrderCountByStatus(@PathVariable OrderStatus status) {
        log.info("Getting order count for status: {}", status);

        Long count = orderService.getOrderCountByStatus(status);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/admin/analytics/total-revenue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Double> getTotalRevenue() {
        log.info("Getting total revenue");

        Double revenue = orderService.getTotalRevenue();
        return ResponseEntity.ok(revenue);
    }

    @GetMapping("/admin/analytics/orders-between-dates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponse>> getOrdersBetweenDates(@RequestParam LocalDateTime startDate,
                                                                     @RequestParam LocalDateTime endDate) {
        log.info("Getting orders between {} and {}", startDate, endDate);

        List<OrderResponse> orders = orderService.getOrdersBetweenDates(startDate, endDate);
        return ResponseEntity.ok(orders);
    }

    // Utility method to extract user ID from authentication
    private Long getUserIdFromAuthentication(Authentication authentication) {
        // This should be implemented based on your JWT token structure
        // For now, assuming username is the user ID or you have a UserDetails implementation
        // that contains the user ID
        return Long.parseLong(authentication.getName()); // Adjust as needed
    }
}
