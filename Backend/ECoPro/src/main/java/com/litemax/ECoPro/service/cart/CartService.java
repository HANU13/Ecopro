package com.litemax.ECoPro.service.cart;

import com.litemax.ECoPro.dto.cart.*;
import com.litemax.ECoPro.entity.auth.User;
import com.litemax.ECoPro.entity.cart.Cart;
import com.litemax.ECoPro.entity.cart.CartDiscount;
import com.litemax.ECoPro.entity.cart.CartItem;
import com.litemax.ECoPro.entity.product.Product;
import com.litemax.ECoPro.entity.product.ProductVariant;
import com.litemax.ECoPro.exception.ResourceNotFoundException;
import com.litemax.ECoPro.exception.ValidationException;
import com.litemax.ECoPro.repository.auth.UserRepository;
import com.litemax.ECoPro.repository.cart.CartItemRepository;
import com.litemax.ECoPro.repository.cart.CartRepository;
import com.litemax.ECoPro.repository.product.ProductRepository;
import com.litemax.ECoPro.repository.product.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;


    public Cart findById(Long cartId){
        Cart cart=cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart Id not found "+cartId));
        return cart;
    }

    // Cart management methods

    public CartResponse getOrCreateCart(String userEmail) {
        log.debug("Getting or creating cart for user: {}", userEmail);
        
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        Cart cart = cartRepository.findByUserIdAndStatus(user.getId(), Cart.CartStatus.ACTIVE)
                .orElse(null);

        if (cart == null) {
            log.debug("Creating new cart for user: {}", userEmail);
            cart = createNewCart(user);
        } else {
            log.debug("Found existing cart: {} for user: {}", cart.getId(), userEmail);
            // Check if cart is expired
            if (cart.isExpired()) {
                cart.setStatus(Cart.CartStatus.EXPIRED);
                cartRepository.save(cart);
                cart = createNewCart(user);
            }
        }

        return convertToResponse(cart);
    }

    public CartResponse getCartBySession(String sessionId) {
        log.debug("Getting cart by session: {}", sessionId);
        
        Cart cart = cartRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for session: " + sessionId));

        return convertToResponse(cart);
    }

    public CartResponse addItemToCart(String userEmail, CartRequest request) {
        log.info("Adding item to cart for user: {} - Product: {}, Quantity: {}", 
                userEmail, request.getProductId(), request.getQuantity());

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        // Get or create cart
        Cart cart = cartRepository.findByUserIdAndStatus(user.getId(), Cart.CartStatus.ACTIVE)
                .orElseGet(() -> createNewCart(user));

        // Validate and get product
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.getProductId()));

        if (product.getStatus() != Product.ProductStatus.ACTIVE) {
            throw new ValidationException("Product is not available for purchase");
        }

        // Validate variant if provided
        ProductVariant variant = null;
        if (request.getVariantId() != null) {
            variant = productVariantRepository.findById(request.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product variant not found: " + request.getVariantId()));

            if (!variant.getProduct().getId().equals(product.getId())) {
                throw new ValidationException("Variant does not belong to the specified product");
            }

            if (!variant.isActive()) {
                throw new ValidationException("Product variant is not available");
            }
        }

        // Check stock availability
        validateStockAvailability(product, variant, request.getQuantity());

        // Check if item already exists in cart
        CartItem existingItem = findExistingCartItem(cart, product, variant);

        if (existingItem != null) {
            // Update existing item
            int newQuantity = existingItem.getQuantity() + request.getQuantity();
            validateStockAvailability(product, variant, newQuantity);

            existingItem.setQuantity(newQuantity);
            existingItem.updateTotalPrice();
            existingItem.setCustomAttributes(request.getCustomAttributes());
            existingItem.setGiftWrap(request.getGiftWrap());
            existingItem.setGiftMessage(request.getGiftMessage());

            cartItemRepository.save(existingItem);
            log.info("Updated existing cart item quantity to: {}", newQuantity);
        } else {
            // Create new cart item
            CartItem cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setVariant(variant);
            cartItem.setQuantity(request.getQuantity());
            cartItem.setUnitPrice(variant != null ? variant.getPrice() : product.getPrice());
            cartItem.setComparePrice(variant != null ? variant.getComparePrice() : product.getComparePrice());
            cartItem.setCustomAttributes(request.getCustomAttributes());
            cartItem.setGiftWrap(request.getGiftWrap());
            cartItem.setGiftMessage(request.getGiftMessage());
            cartItem.updateTotalPrice();

            cart.addItem(cartItem);
            cartItemRepository.save(cartItem);
            log.info("Added new cart item with quantity: {}", request.getQuantity());
        }

        cart.recalculateCart();
        cart = cartRepository.save(cart);

        log.info("Item added to cart successfully. Cart total: {}", cart.getTotalAmount());
        return convertToResponse(cart);
    }

    public CartResponse updateCartItem(String userEmail, Long itemId, Integer quantity) {
        log.info("Updating cart item: {} to quantity: {} for user: {}", itemId, quantity, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + itemId));

        // Verify ownership
        if (!cartItem.getCart().getUser().getId().equals(user.getId())) {
            throw new ValidationException("Cart item does not belong to the user");
        }

        if (quantity <= 0) {
            // Remove item if quantity is 0 or negative
            return removeCartItem(userEmail, itemId);
        }

        // Validate stock availability
        validateStockAvailability(cartItem.getProduct(), cartItem.getVariant(), quantity);

        cartItem.setQuantity(quantity);
        cartItem.updateTotalPrice();
        cartItemRepository.save(cartItem);

        Cart cart = cartItem.getCart();
        cart.recalculateCart();
        cartRepository.save(cart);

        log.info("Cart item updated successfully. New quantity: {}", quantity);
        return convertToResponse(cart);
    }

    public CartResponse removeCartItem(String userEmail, Long itemId) {
        log.info("Removing cart item: {} for user: {}", itemId, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + itemId));

        // Verify ownership
        if (!cartItem.getCart().getUser().getId().equals(user.getId())) {
            throw new ValidationException("Cart item does not belong to the user");
        }

        Cart cart = cartItem.getCart();
        cart.removeItem(cartItem);
        cartItemRepository.delete(cartItem);

        cart.recalculateCart();
        cartRepository.save(cart);

        log.info("Cart item removed successfully");
        return convertToResponse(cart);
    }

    public CartResponse clearCart(String userEmail) {
        log.info("Clearing cart for user: {}", userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        Cart cart = cartRepository.findByUserIdAndStatus(user.getId(), Cart.CartStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("No active cart found for user"));

        cart.clearItems();
        cartItemRepository.deleteByCartId(cart.getId());
        cart.recalculateCart();
        cartRepository.save(cart);

        log.info("Cart cleared successfully for user: {}", userEmail);
        return convertToResponse(cart);
    }

    public CartSummaryResponse getCartSummary(String userEmail) {
        log.debug("Getting cart summary for user: {}", userEmail);

        CartResponse cart = getOrCreateCart(userEmail);
        return buildCartSummary(cart);
    }

    public void mergeGuestCart(String sessionId, String userEmail) {
        log.info("Merging guest cart (session: {}) with user cart: {}", sessionId, userEmail);

        Cart guestCart = cartRepository.findBySessionId(sessionId).orElse(null);
        if (guestCart == null || guestCart.isEmpty()) {
            log.debug("No guest cart found or cart is empty, skipping merge");
            return;
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        Cart userCart = cartRepository.findByUserIdAndStatus(user.getId(), Cart.CartStatus.ACTIVE)
                .orElseGet(() -> createNewCart(user));

        // Merge items from guest cart to user cart
        for (CartItem guestItem : guestCart.getItems()) {
            CartItem existingItem = findExistingCartItem(userCart, guestItem.getProduct(), guestItem.getVariant());

            if (existingItem != null) {
                // Merge quantities
                int newQuantity = existingItem.getQuantity() + guestItem.getQuantity();
                if (hasEnoughStock(guestItem.getProduct(), guestItem.getVariant(), newQuantity)) {
                    existingItem.setQuantity(newQuantity);
                    existingItem.updateTotalPrice();
                    cartItemRepository.save(existingItem);
                }
            } else {
                // Add new item to user cart
                CartItem newItem = new CartItem();
                newItem.setCart(userCart);
                newItem.setProduct(guestItem.getProduct());
                newItem.setVariant(guestItem.getVariant());
                newItem.setQuantity(guestItem.getQuantity());
                newItem.setUnitPrice(guestItem.getUnitPrice());
                newItem.setComparePrice(guestItem.getComparePrice());
                newItem.setCustomAttributes(guestItem.getCustomAttributes());
                newItem.setGiftWrap(guestItem.getGiftWrap());
                newItem.setGiftMessage(guestItem.getGiftMessage());
                newItem.updateTotalPrice();

                userCart.addItem(newItem);
                cartItemRepository.save(newItem);
            }
        }

        userCart.recalculateCart();
        cartRepository.save(userCart);

        // Delete guest cart
        cartRepository.delete(guestCart);

        log.info("Successfully merged guest cart with user cart. Items count: {}", userCart.getItemsCount());
    }

    // Cart validation and stock checking

    public void validateCartBeforeCheckout(String userEmail) {
        log.debug("Validating cart before checkout for user: {}", userEmail);

        CartResponse cart = getOrCreateCart(userEmail);

        if (cart.isEmpty()) {
            throw new ValidationException("Cart is empty");
        }

        for (CartItemResponse item : cart.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ValidationException("Product no longer available: " + item.getProductName()));

            if (product.getStatus() != Product.ProductStatus.ACTIVE) {
                throw new ValidationException("Product is no longer available: " + item.getProductName());
            }

            ProductVariant variant = null;
            if (item.getVariantId() != null) {
                variant = productVariantRepository.findById(item.getVariantId()).orElse(null);
                if (variant == null || !variant.isActive()) {
                    throw new ValidationException("Product variant is no longer available: " + item.getDisplayName());
                }
            }

            if (!hasEnoughStock(product, variant, item.getQuantity())) {
                throw new ValidationException("Insufficient stock for: " + item.getDisplayName());
            }
        }

        log.debug("Cart validation completed successfully");
    }

    public List<String> getCartValidationMessages(String userEmail) {
        log.debug("Getting cart validation messages for user: {}", userEmail);

        CartResponse cart = getOrCreateCart(userEmail);
        List<String> messages = new java.util.ArrayList<>();

        if (cart.isEmpty()) {
            messages.add("Your cart is empty");
            return messages;
        }

        for (CartItemResponse item : cart.getItems()) {
            if (!item.isAvailable()) {
                messages.add(item.getDisplayName() + " is no longer available");
            } else if (!item.isInStock()) {
                messages.add(item.getDisplayName() + " is out of stock");
            } else if (item.getAvailableQuantity() != null &&
                    item.getQuantity() > item.getAvailableQuantity()) {
                messages.add("Only " + item.getAvailableQuantity() + " of " +
                        item.getDisplayName() + " available");
            }
        }

        return messages;
    }

    // Scheduled maintenance methods

    @Transactional
    public void expireOldCarts() {
        log.info("Expiring old carts");
        cartRepository.expireOldCarts(LocalDateTime.now());
    }

    @Transactional
    public void markAbandonedCarts() {
        log.info("Marking abandoned carts");
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(3); // 3 days of inactivity
        cartRepository.markCartsAsAbandoned(cutoffDate);
    }

    // Private helper methods

    private Cart createNewCart(User user) {
        Cart cart = new Cart();
        cart.setUser(user);
        cart.setStatus(Cart.CartStatus.ACTIVE);
        cart.setExpiresAt(LocalDateTime.now().plusDays(30)); // Cart expires in 30 days
        return cartRepository.save(cart);
    }

    private CartItem findExistingCartItem(Cart cart, Product product, ProductVariant variant) {
        if (variant != null) {
            return cart.findItemByVariantId(variant.getId());
        } else {
            return cart.findItemByProductId(product.getId());
        }
    }

    private void validateStockAvailability(Product product, ProductVariant variant, Integer quantity) {
        if (!hasEnoughStock(product, variant, quantity)) {
            String itemName = product.getName();
            if (variant != null) {
                itemName += " - " + variant.getDisplayName();
            }
            throw new ValidationException("Insufficient stock for " + itemName + ". Requested: " + quantity);
        }
    }

    private boolean hasEnoughStock(Product product, ProductVariant variant, Integer quantity) {
        if (variant != null) {
            return variant.getInventoryQuantory() >= quantity ||
                    variant.getInventoryPolicy() == ProductVariant.InventoryPolicy.CONTINUE;
        } else {
            return !product.isTrackInventory() ||
                    product.getInventoryQuantity() >= quantity ||
                    product.isAllowBackorders();
        }
    }

    private CartSummaryResponse buildCartSummary(CartResponse cart) {
        List<CartSummaryResponse.CartRecommendation> recommendations = new java.util.ArrayList<>();
        List<String> messages = new java.util.ArrayList<>();

        // Check for free shipping eligibility
        BigDecimal freeShippingThreshold = new BigDecimal("75.00");
        boolean freeShippingEligible = cart.getSubtotal().compareTo(freeShippingThreshold) >= 0;
        BigDecimal amountForFreeShipping = freeShippingEligible ?
                BigDecimal.ZERO : freeShippingThreshold.subtract(cart.getSubtotal());

        if (!freeShippingEligible && amountForFreeShipping.compareTo(new BigDecimal("25.00")) <= 0) {
            messages.add("Add " + formatPrice(amountForFreeShipping) + " more for free shipping!");
        }

        // Check for out of stock items
        boolean hasOutOfStockItems = cart.getItems().stream()
                .anyMatch(item -> !item.isInStock());

        if (hasOutOfStockItems) {
            messages.add("Some items in your cart are out of stock");
        }

        // Check for gift items
        boolean hasGiftItems = cart.getItems().stream()
                .anyMatch(CartItemResponse::getGiftWrap);

        return CartSummaryResponse.builder()
                .itemsCount(cart.getItemsCount())
                .uniqueItemsCount(cart.getItems().size())
                .subtotal(cart.getSubtotal())
                .discountAmount(cart.getDiscountAmount())
                .taxAmount(cart.getTaxAmount())
                .shippingAmount(BigDecimal.ZERO) // Would be calculated based on shipping rules
                .totalAmount(cart.getTotalAmount())
                .savingsAmount(cart.getSavingsAmount())
                .couponCode(cart.getCouponCode())
                .formattedSubtotal(formatPrice(cart.getSubtotal()))
                .formattedDiscount(formatPrice(cart.getDiscountAmount()))
                .formattedTax(formatPrice(cart.getTaxAmount()))
                .formattedShipping(formatPrice(BigDecimal.ZERO))
                .formattedTotal(formatPrice(cart.getTotalAmount()))
                .formattedSavings(formatPrice(cart.getSavingsAmount()))
                .hasItems(!cart.isEmpty())
                .hasDiscounts(cart.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0)
                .hasOutOfStockItems(hasOutOfStockItems)
                .hasGiftItems(hasGiftItems)
                .isShippingRequired(true) // Would be calculated based on cart contents
                .freeShippingEligible(freeShippingEligible)
                .freeShippingThreshold(freeShippingThreshold)
                .amountForFreeShipping(amountForFreeShipping)
                .messages(messages)
                .recommendations(recommendations)
                .build();
    }

    private CartResponse convertToResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(this::convertToItemResponse)
                .collect(Collectors.toList());

        List<CartDiscountResponse> discounts = cart.getDiscounts().stream()
                .map(this::convertToDiscountResponse)
                .collect(Collectors.toList());

        CartResponse.CartSummary summary = CartResponse.CartSummary.builder()
                .totalItems(cart.getItemsCount())
                .uniqueItems(items.size())
                .averageItemPrice(items.isEmpty() ? BigDecimal.ZERO :
                        cart.getSubtotal().divide(new BigDecimal(cart.getItemsCount()), 2, java.math.RoundingMode.HALF_UP))
                .hasOutOfStockItems(items.stream().anyMatch(item -> !item.isInStock()))
                .hasGiftItems(items.stream().anyMatch(CartItemResponse::getGiftWrap))
                .estimatedDelivery("3-5 business days")
                .build();

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUser() != null ? cart.getUser().getId() : null)
                .sessionId(cart.getSessionId())
                .status(cart.getStatus().toString())
                .itemsCount(cart.getItemsCount())
                .subtotal(cart.getSubtotal())
                .discountAmount(cart.getDiscountAmount())
                .taxAmount(cart.getTaxAmount())
                .totalAmount(cart.getTotalAmount())
                .savingsAmount(cart.getSavingsAmount())
                .couponCode(cart.getCouponCode())
                .notes(cart.getNotes())
                .expiresAt(cart.getExpiresAt())
                .items(items)
                .discounts(discounts)
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .isEmpty(cart.isEmpty())
                .isExpired(cart.isExpired())
                .formattedSubtotal(formatPrice(cart.getSubtotal()))
                .formattedTotal(formatPrice(cart.getTotalAmount()))
                .formattedSavings(formatPrice(cart.getSavingsAmount()))
                .summary(summary)
                .build();
    }

    private CartItemResponse convertToItemResponse(CartItem item) {
        Product product = item.getProduct();
        ProductVariant variant = item.getVariant();

        Integer availableQuantity = null;
        if (variant != null && variant.getInventoryQuantory() != null) {
            availableQuantity = variant.getInventoryQuantory();
        } else if (product.isTrackInventory() && product.getInventoryQuantity() != null) {
            availableQuantity = product.getInventoryQuantity();
        }

        return CartItemResponse.builder()
                .id(item.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productSlug(product.getSlug())
                .variantId(variant != null ? variant.getId() : null)
                .variantName(variant != null ? variant.getName() : null)
                .displayName(item.getDisplayName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .comparePrice(item.getComparePrice())
                .totalPrice(item.getTotalPrice())
                .savingsAmount(item.getSavingsAmount())
                .imageUrl(item.getImageUrl())
                .customAttributes(item.getCustomAttributes())
                .giftWrap(item.getGiftWrap())
                .giftMessage(item.getGiftMessage())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .inStock(item.isInStock())
                .available(product.getStatus() == Product.ProductStatus.ACTIVE &&
                        (variant == null || variant.isActive()))
                .availableQuantity(availableQuantity)
                .stockStatus(getStockStatus(item))
                .formattedUnitPrice(formatPrice(item.getUnitPrice()))
                .formattedTotalPrice(formatPrice(item.getTotalPrice()))
                .formattedSavings(formatPrice(item.getSavingsAmount()))
                .hasDiscount(item.getSavingsAmount().compareTo(BigDecimal.ZERO) > 0)
                .productUrl("/products/" + product.getSlug())
                .build();
    }

    private CartDiscountResponse convertToDiscountResponse(CartDiscount discount) {
        return CartDiscountResponse.builder()
                .id(discount.getId())
                .discountCode(discount.getDiscountCode())
                .discountType(discount.getDiscountType().toString())
                .discountValue(discount.getDiscountValue())
                .discountAmount(discount.getDiscountAmount())
                .title(discount.getTitle())
                .description(discount.getDescription())
                .createdAt(discount.getCreatedAt())
                .formattedDiscountAmount(formatPrice(discount.getDiscountAmount()))
                .isPercentage(discount.getDiscountType() == CartDiscount.DiscountType.PERCENTAGE)
                .displayText(buildDiscountDisplayText(discount))
                .build();
    }

    private String buildDiscountDisplayText(CartDiscount discount) {
        return switch (discount.getDiscountType()) {
            case PERCENTAGE -> discount.getDiscountValue() + "% off";
            case FIXED_AMOUNT -> formatPrice(discount.getDiscountValue()) + " off";
            case FREE_SHIPPING -> "Free shipping";
            case BUY_X_GET_Y -> "Buy X Get Y";
        };
    }

    private String getStockStatus(CartItem item) {
        if (!item.isInStock()) return "OUT_OF_STOCK";
        if (!item.hasEnoughStock(item.getQuantity())) return "INSUFFICIENT_STOCK";
        return "IN_STOCK";
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) return "$0.00";
        return String.format("$%.2f", price);
    }
}