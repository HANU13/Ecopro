package com.litemax.ECoPro.service.order;



//@Service
//@RequiredArgsConstructor
//@Slf4j
public class OrderService1 {
//    
//    private final OrderRepository orderRepository;
//    private final OrderItemRepository orderItemRepository;
//    private final CartRepository cartService;
//    private final UserRepository userService;
//    private final EmailService emailService;
//    
//    @Transactional
//    public OrderResponse createOrder(CreateOrderRequest request, Long userId) {
//        log.info("Creating order for user: {} with cart: {}", userId, request.getCartId());
//        
//        try {
//            // Validate user
//            User user = userService.getById(userId);
//            
//            // Get cart and validate
//            Cart cart = cartService.getById(request.getCartId());
//            if (!cart.getUser().getId().equals(userId)) {
//                throw new ValidationException("Cart does not belong to the user");
//            }
//            
//            if (cart.getItems().isEmpty()) {
//                throw new ValidationException("Cart is empty");
//            }
//            
//            // Create order
//            Order order = new Order();
//            order.setUser(user);
//            order.setStatus(Order.OrderStatus.PLACED);
//            
//            // Set addresses
//            setShippingAddress(order, request);
//            setBillingAddress(order, request);
//            
//            order.setCustomerNotes(request.getCustomerNotes());
//            
//            // Calculate amounts
//            calculateOrderAmounts(order, cart);
//            
//            // Save order
//            order = orderRepository.save(order);
//            log.info("Order created with ID: {} and order number: {}", order.getId(), order.getOrderNumber());
//            
//            // Create order items
//            createOrderItems(order, cart);
//            
//            // Clear cart after successful order creation
//            cartService.deleteById(cart.getId());
//            
//            // Send confirmation email
//            emailService.sendOrderConfirmation(user.getEmail(), order);
//            
//            log.info("Order creation completed successfully for order: {}", order.getOrderNumber());
//            return convertToOrderResponse(order);
//            
//        } catch (Exception e) {
//            log.error("Error creating order for user: {} with cart: {}", userId, request.getCartId(), e);
//            throw e;
//        }
//    }
//    
//    @Transactional(readOnly = true)
//    public OrderResponse getOrderById(Long orderId, Long userId, String userRole) {
//        log.info("Getting order: {} for user: {} with role: {}", orderId, userId, userRole);
//        
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
//        
//        // Check permissions
//        if (!"ADMIN".equals(userRole) && !"SELLER".equals(userRole)) {
//            if (!order.getUser().getId().equals(userId)) {
//                throw new ValidationException("Access denied to this order");
//            }
//        }
//        
//        return convertToOrderResponse(order);
//    }
//    
//    @Transactional(readOnly = true)
//    public Page<OrderResponse> getUserOrders(Long userId, Pageable pageable) {
//        log.info("Getting orders for user: {} with page: {}", userId, pageable.getPageNumber());
//        
//        Page<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
//        return orders.map(this::convertToOrderResponse);
//    }
//    
//    @Transactional(readOnly = true)
//    public Page<OrderResponse> getAllOrders(Pageable pageable) {
//        log.info("Getting all orders with page: {}", pageable.getPageNumber());
//        
//        Page<Order> orders = orderRepository.findAll(pageable);
//        return orders.map(this::convertToOrderResponse);
//    }
//    
//    @Transactional(readOnly = true)
//    public Page<OrderResponse> getOrdersByStatus(Order.OrderStatus status, Pageable pageable) {
//        log.info("Getting orders with status: {} and page: {}", status, pageable.getPageNumber());
//        
//        Page<Order> orders = orderRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
//        return orders.map(this::convertToOrderResponse);
//    }
//    
//    @Transactional
//    public OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request, Long adminUserId) {
//        log.info("Updating order: {} status to: {} by admin: {}", orderId, request.getStatus(), adminUserId);
//        
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
//        
//        Order.OrderStatus oldStatus = order.getStatus();
//        order.setStatus(request.getStatus());
//        order.setAdminNotes(request.getAdminNotes());
//        
//        if (request.getEstimatedDeliveryDate() != null) {
//            order.setEstimatedDeliveryDate(request.getEstimatedDeliveryDate());
//        }
//        
//        if (request.getActualDeliveryDate() != null) {
//            order.setActualDeliveryDate(request.getActualDeliveryDate());
//        }
//        
//        order = orderRepository.save(order);
//        
//        // Send notification email on status change
//        if (!oldStatus.equals(request.getStatus())) {
//            emailService.sendOrderStatusUpdate(order.getUser().getEmail(), order, oldStatus, request.getStatus());
//        }
//        
//        log.info("Order: {} status updated from {} to {} successfully", orderId, oldStatus, request.getStatus());
//        return convertToOrderResponse(order);
//    }
//    
//    @Transactional
//    public void cancelOrder(Long orderId, Long userId, String userRole) {
//        log.info("Cancelling order: {} by user: {} with role: {}", orderId, userId, userRole);
//        
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
//        
//        // Check permissions
//        if (!"ADMIN".equals(userRole)) {
//            if (!order.getUser().getId().equals(userId)) {
//                throw new ValidationException("Access denied to cancel this order");
//            }
//        }
//        
//        // Validate cancellation
//        if (order.getStatus() == Order.OrderStatus.DELIVERED || 
//            order.getStatus() == Order.OrderStatus.CANCELLED) {
//            throw new ValidationException("Cannot cancel order in current status: " + order.getStatus());
//        }
//        
//        Order.OrderStatus oldStatus = order.getStatus();
//        order.setStatus(Order.OrderStatus.CANCELLED);
//        orderRepository.save(order);
//        
//        // Send cancellation email
//        emailService.sendOrderCancellation(order.getUser().getEmail(), order);
//        
//        log.info("Order: {} cancelled successfully. Previous status: {}", orderId, oldStatus);
//    }
//    
//    @Transactional(readOnly = true)
//    public List<OrderResponse> getOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
//        log.info("Getting orders between {} and {}", startDate, endDate);
//        
//        List<Order> orders = orderRepository.findOrdersByDateRange(startDate, endDate);
//        return orders.stream()
//                .map(this::convertToOrderResponse)
//                .collect(Collectors.toList());
//    }
//    
//    // Private helper methods
//    
//    private void setShippingAddress(Order order, CreateOrderRequest request) {
//        order.setShippingAddressLine1(request.getShippingAddressLine1());
//        order.setShippingAddressLine2(request.getShippingAddressLine2());
//        order.setShippingCity(request.getShippingCity());
//        order.setShippingState(request.getShippingState());
//        order.setShippingPostalCode(request.getShippingPostalCode());
//        order.setShippingCountry(request.getShippingCountry());
//    }
//    
//    private void setBillingAddress(Order order, CreateOrderRequest request) {
//        if (request.getUseSameAddressForBilling()) {
//            order.setBillingAddressLine1(request.getShippingAddressLine1());
//            order.setBillingAddressLine2(request.getShippingAddressLine2());
//            order.setBillingCity(request.getShippingCity());
//            order.setBillingState(request.getShippingState());
//            order.setBillingPostalCode(request.getShippingPostalCode());
//            order.setBillingCountry(request.getShippingCountry());
//        } else {
//            order.setBillingAddressLine1(request.getBillingAddressLine1());
//            order.setBillingAddressLine2(request.getBillingAddressLine2());
//            order.setBillingCity(request.getBillingCity());
//            order.setBillingState(request.getBillingState());
//            order.setBillingPostalCode(request.getBillingPostalCode());
//            order.setBillingCountry(request.getBillingCountry());
//        }
//    }
//    
//    private void calculateOrderAmounts(Order order, Cart cart) {
//        BigDecimal totalAmount = cart.getItems().stream()
//                .map(item -> item.getComparePrice().multiply(BigDecimal.valueOf(item.getQuantity())))
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//        
//        order.setTotalAmount(totalAmount);
//        
//        // Calculate tax (example: 10%)
//        BigDecimal taxAmount = totalAmount.multiply(BigDecimal.valueOf(0.10));
//        order.setTaxAmount(taxAmount);
//        
//        // Calculate shipping (example: flat rate of $10)
//        BigDecimal shippingAmount = BigDecimal.valueOf(10.00);
//        order.setShippingAmount(shippingAmount);
//        
//        // Calculate final amount
//        BigDecimal finalAmount = totalAmount.add(taxAmount).add(shippingAmount).subtract(order.getDiscountAmount());
//        order.setFinalAmount(finalAmount);
//    }
//    
//    private void createOrderItems(Order order, Cart cart) {
//        for (CartItem cartItem : cart.getItems()) {
//            OrderItem orderItem = new OrderItem();
//            orderItem.setOrder(order);
//            orderItem.setProduct(cartItem.getProduct());
//            orderItem.setProductVariant(cartItem.getVariant());
//            orderItem.setQuantity(cartItem.getQuantity());
//            orderItem.setUnitPrice(cartItem.getTotalPrice());
//            orderItem.setTotalPrice(cartItem.getComparePrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
//            
//            // Store product details for historical accuracy
//            orderItem.setProductName(cartItem.getProduct().getName());
//            orderItem.setProductSku(cartItem.getProduct().getSku());
//            if (cartItem.getVariant() != null) {
//                orderItem.setVariantName(cartItem.getVariant().getName());
//            }
//            
//            orderItemRepository.save(orderItem);
//            order.getOrderItems().add(orderItem);
//        }
//    }
//    
//    private OrderResponse convertToOrderResponse(Order order) {
//        OrderResponse response = new OrderResponse();
//        response.setId(order.getId());
//        response.setOrderNumber(order.getOrderNumber());
//        response.setStatus(order.getStatus());
//        response.setTotalAmount(order.getTotalAmount());
//        response.setDiscountAmount(order.getDiscountAmount());
//        response.setTaxAmount(order.getTaxAmount());
//        response.setShippingAmount(order.getShippingAmount());
//        response.setFinalAmount(order.getFinalAmount());
//        response.setCustomerNotes(order.getCustomerNotes());
//        response.setAdminNotes(order.getAdminNotes());
//        response.setEstimatedDeliveryDate(order.getEstimatedDeliveryDate());
//        response.setActualDeliveryDate(order.getActualDeliveryDate());
//        response.setCreatedAt(order.getCreatedAt());
//        response.setUpdatedAt(order.getUpdatedAt());
//        
//        // Set addresses
//        OrderResponse.AddressInfo shippingAddress = new OrderResponse.AddressInfo();
//        shippingAddress.setAddressLine1(order.getShippingAddressLine1());
//        shippingAddress.setAddressLine2(order.getShippingAddressLine2());
//        shippingAddress.setCity(order.getShippingCity());
//        shippingAddress.setState(order.getShippingState());
//        shippingAddress.setPostalCode(order.getShippingPostalCode());
//        shippingAddress.setCountry(order.getShippingCountry());
//        response.setShippingAddress(shippingAddress);
//        
//        OrderResponse.AddressInfo billingAddress = new OrderResponse.AddressInfo();
//        billingAddress.setAddressLine1(order.getBillingAddressLine1());
//        billingAddress.setAddressLine2(order.getBillingAddressLine2());
//        billingAddress.setCity(order.getBillingCity());
//        billingAddress.setState(order.getBillingState());
//        billingAddress.setPostalCode(order.getBillingPostalCode());
//        billingAddress.setCountry(order.getBillingCountry());
//        response.setBillingAddress(billingAddress);
//        
//        // Convert order items
//        List<OrderItemResponse> orderItems = order.getOrderItems().stream()
//                .map(this::convertToOrderItemResponse)
//                .collect(Collectors.toList());
//        response.setOrderItems(orderItems);
//        
//        return response;
//    }
//    
//    private OrderItemResponse convertToOrderItemResponse(OrderItem orderItem) {
//        OrderItemResponse response = new OrderItemResponse();
//        response.setId(orderItem.getId());
//        response.setProductId(orderItem.getProduct().getId());
//        response.setProductVariantId(orderItem.getProductVariant() != null ? 
//                                   orderItem.getProductVariant().getId() : null);
//        response.setProductName(orderItem.getProductName());
//        response.setProductSku(orderItem.getProductSku());
//        response.setVariantName(orderItem.getVariantName());
//        response.setQuantity(orderItem.getQuantity());
//        response.setUnitPrice(orderItem.getUnitPrice());
//        response.setTotalPrice(orderItem.getTotalPrice());
//        return response;
//    }
}