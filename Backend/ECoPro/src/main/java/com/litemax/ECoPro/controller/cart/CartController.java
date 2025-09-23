package com.litemax.ECoPro.controller.cart;

import com.litemax.ECoPro.dto.cart.*;
import com.litemax.ECoPro.service.cart.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Shopping Cart", description = "Shopping cart management APIs")
@SecurityRequirement(name = "bearerAuth")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(
        summary = "Get user cart",
        description = "Retrieves the current user's shopping cart"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cart retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access")
    })
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("Getting cart for user: {}", userDetails.getUsername());
        CartResponse cart = cartService.getOrCreateCart(userDetails.getUsername());
        return ResponseEntity.ok(cart);
    }

    @PostMapping("/items")
    @Operation(
        summary = "Add item to cart",
        description = "Adds a product to the user's shopping cart"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Item added successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Product not found"),
        @ApiResponse(responseCode = "409", description = "Insufficient stock")
    })
    public ResponseEntity<CartResponse> addItemToCart(
            @Parameter(description = "Cart item details", required = true)
            @Valid @RequestBody CartRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Adding item to cart for user: {} - Product: {}, Quantity: {}", 
                userDetails.getUsername(), request.getProductId(), request.getQuantity());
        
        CartResponse cart = cartService.addItemToCart(userDetails.getUsername(), request);
        return ResponseEntity.ok(cart);
    }

    @PutMapping("/items/{itemId}")
    @Operation(
        summary = "Update cart item quantity",
        description = "Updates the quantity of an item in the cart"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Item updated successfully"),
        @ApiResponse(responseCode = "404", description = "Cart item not found"),
        @ApiResponse(responseCode = "409", description = "Insufficient stock")
    })
    public ResponseEntity<CartResponse> updateCartItem(
            @Parameter(description = "Cart item ID", required = true)
            @PathVariable Long itemId,
            @Parameter(description = "New quantity", required = true)
            @RequestParam Integer quantity,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Updating cart item: {} to quantity: {} for user: {}", 
                itemId, quantity, userDetails.getUsername());
        
        CartResponse cart = cartService.updateCartItem(userDetails.getUsername(), itemId, quantity);
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(
        summary = "Remove item from cart",
        description = "Removes an item from the user's shopping cart"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Item removed successfully"),
        @ApiResponse(responseCode = "404", description = "Cart item not found")
    })
    public ResponseEntity<CartResponse> removeCartItem(
            @Parameter(description = "Cart item ID", required = true)
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Removing cart item: {} for user: {}", itemId, userDetails.getUsername());
        
        CartResponse cart = cartService.removeCartItem(userDetails.getUsername(), itemId);
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping
    @Operation(
        summary = "Clear cart",
        description = "Removes all items from the user's shopping cart"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cart cleared successfully")
    })
    public ResponseEntity<CartResponse> clearCart(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("Clearing cart for user: {}", userDetails.getUsername());
        
        CartResponse cart = cartService.clearCart(userDetails.getUsername());
        return ResponseEntity.ok(cart);
    }

    @GetMapping("/summary")
    @Operation(
        summary = "Get cart summary",
        description = "Retrieves cart summary with totals and recommendations"
    )
    public ResponseEntity<CartSummaryResponse> getCartSummary(@AuthenticationPrincipal UserDetails userDetails) {
        log.debug("Getting cart summary for user: {}", userDetails.getUsername());
        
        CartSummaryResponse summary = cartService.getCartSummary(userDetails.getUsername());
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/validate")
    @Operation(
        summary = "Validate cart",
        description = "Validates cart items for checkout readiness"
    )
    public ResponseEntity<Map<String, Object>> validateCart(@AuthenticationPrincipal UserDetails userDetails) {
        log.debug("Validating cart for user: {}", userDetails.getUsername());
        
        try {
            cartService.validateCartBeforeCheckout(userDetails.getUsername());
            return ResponseEntity.ok(Map.of("valid", true, "message", "Cart is valid for checkout"));
        } catch (Exception e) {
            List<String> messages = cartService.getCartValidationMessages(userDetails.getUsername());
            return ResponseEntity.ok(Map.of(
                "valid", false,
                "message", e.getMessage(),
                "issues", messages
            ));
        }
    }

    @PostMapping("/merge")
    @Operation(
        summary = "Merge guest cart",
        description = "Merges a guest cart with the user's cart after login"
    )
    public ResponseEntity<CartResponse> mergeGuestCart(
            @Parameter(description = "Guest session ID", required = true)
            @RequestParam String sessionId,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request) {
        
        log.info("Merging guest cart (session: {}) for user: {}", sessionId, userDetails.getUsername());
        
        cartService.mergeGuestCart(sessionId, userDetails.getUsername());
        CartResponse cart = cartService.getOrCreateCart(userDetails.getUsername());
        return ResponseEntity.ok(cart);
    }

    @GetMapping("/count")
    @Operation(
        summary = "Get cart items count",
        description = "Returns the number of items in the user's cart"
    )
    public ResponseEntity<Map<String, Object>> getCartItemsCount(@AuthenticationPrincipal UserDetails userDetails) {
        log.debug("Getting cart items count for user: {}", userDetails.getUsername());
        
        CartResponse cart = cartService.getOrCreateCart(userDetails.getUsername());
        return ResponseEntity.ok(Map.of(
            "itemsCount", cart.getItemsCount(),
            "uniqueItemsCount", cart.getItems().size()
        ));
    }
}