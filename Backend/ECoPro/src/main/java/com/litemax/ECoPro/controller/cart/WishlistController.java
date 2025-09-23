package com.litemax.ECoPro.controller.cart;

import com.litemax.ECoPro.dto.cart.WishlistRequest;
import com.litemax.ECoPro.dto.cart.WishlistResponse;
import com.litemax.ECoPro.service.cart.CartService;
import com.litemax.ECoPro.service.cart.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Wishlist", description = "User wishlist management APIs")
@SecurityRequirement(name = "bearerAuth")
public class WishlistController {

    private final WishlistService wishlistService;
    private final CartService cartService;

    @GetMapping
    @Operation(
        summary = "Get user wishlist",
        description = "Retrieves the current user's wishlist items"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Wishlist retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access")
    })
    public ResponseEntity<List<WishlistResponse>> getWishlist(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("Getting wishlist for user: {}", userDetails.getUsername());
        List<WishlistResponse> wishlist = wishlistService.getUserWishlist(userDetails.getUsername());
        return ResponseEntity.ok(wishlist);
    }

    @GetMapping("/paginated")
    @Operation(
        summary = "Get paginated wishlist",
        description = "Retrieves the user's wishlist with pagination"
    )
    public ResponseEntity<Page<WishlistResponse>> getWishlistPaginated(
            @Parameter(description = "Page number", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort by field", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction", example = "desc")
            @RequestParam(defaultValue = "desc") String sortDirection,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.debug("Getting paginated wishlist for user: {} - page: {}, size: {}", 
                userDetails.getUsername(), page, size);
        
        Page<WishlistResponse> wishlist = wishlistService.getUserWishlistPaginated(
                userDetails.getUsername(), page, size, sortBy, sortDirection);
        return ResponseEntity.ok(wishlist);
    }

    @PostMapping
    @Operation(
        summary = "Add item to wishlist",
        description = "Adds a product to the user's wishlist"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Item added successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Product not found"),
        @ApiResponse(responseCode = "409", description = "Item already in wishlist")
    })
    public ResponseEntity<WishlistResponse> addToWishlist(
            @Parameter(description = "Wishlist item details", required = true)
            @Valid @RequestBody WishlistRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Adding item to wishlist for user: {} - Product: {}", 
                userDetails.getUsername(), request.getProductId());
        
        WishlistResponse wishlistItem = wishlistService.addToWishlist(userDetails.getUsername(), request);
        return new ResponseEntity<>(wishlistItem, HttpStatus.CREATED);
    }

    @PutMapping("/{wishlistId}")
    @Operation(
        summary = "Update wishlist item",
        description = "Updates a wishlist item's details"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Item updated successfully"),
        @ApiResponse(responseCode = "404", description = "Wishlist item not found")
    })
    public ResponseEntity<WishlistResponse> updateWishlistItem(
            @Parameter(description = "Wishlist item ID", required = true)
            @PathVariable Long wishlistId,
            @Parameter(description = "Updated wishlist item details", required = true)
            @Valid @RequestBody WishlistRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Updating wishlist item: {} for user: {}", wishlistId, userDetails.getUsername());
        
        WishlistResponse wishlistItem = wishlistService.updateWishlistItem(
                userDetails.getUsername(), wishlistId, request);
        return ResponseEntity.ok(wishlistItem);
    }

    @DeleteMapping("/{wishlistId}")
    @Operation(
        summary = "Remove item from wishlist",
        description = "Removes an item from the user's wishlist"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Item removed successfully"),
        @ApiResponse(responseCode = "404", description = "Wishlist item not found")
    })
    public ResponseEntity<Void> removeFromWishlist(
            @Parameter(description = "Wishlist item ID", required = true)
            @PathVariable Long wishlistId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Removing item from wishlist: {} for user: {}", wishlistId, userDetails.getUsername());
        
        wishlistService.removeFromWishlist(userDetails.getUsername(), wishlistId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/product/{productId}")
    @Operation(
        summary = "Remove product from wishlist",
        description = "Removes a product (and optionally variant) from wishlist"
    )
    public ResponseEntity<Void> removeProductFromWishlist(
            @Parameter(description = "Product ID", required = true)
            @PathVariable Long productId,
            @Parameter(description = "Variant ID (optional)")
            @RequestParam(required = false) Long variantId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Removing product {} (variant: {}) from wishlist for user: {}", 
                productId, variantId, userDetails.getUsername());
        
        wishlistService.removeFromWishlistByProduct(userDetails.getUsername(), productId, variantId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(
        summary = "Clear wishlist",
        description = "Removes all items from the user's wishlist"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Wishlist cleared successfully")
    })
    public ResponseEntity<Void> clearWishlist(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("Clearing wishlist for user: {}", userDetails.getUsername());
        
        wishlistService.clearWishlist(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check/{productId}")
    @Operation(
        summary = "Check if product is in wishlist",
        description = "Checks if a product is already in the user's wishlist"
    )
    public ResponseEntity<Map<String, Boolean>> checkProductInWishlist(
            @Parameter(description = "Product ID", required = true)
            @PathVariable Long productId,
            @Parameter(description = "Variant ID (optional)")
            @RequestParam(required = false) Long variantId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.debug("Checking if product {} (variant: {}) is in wishlist for user: {}", 
                productId, variantId, userDetails.getUsername());
        
        boolean inWishlist = wishlistService.isInWishlist(userDetails.getUsername(), productId, variantId);
        return ResponseEntity.ok(Map.of("inWishlist", inWishlist));
    }

    @GetMapping("/count")
    @Operation(
        summary = "Get wishlist items count",
        description = "Returns the number of items in the user's wishlist"
    )
    public ResponseEntity<Map<String, Long>> getWishlistCount(@AuthenticationPrincipal UserDetails userDetails) {
        log.debug("Getting wishlist count for user: {}", userDetails.getUsername());
        
        long count = wishlistService.getWishlistCount(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping("/priority/{priority}")
    @Operation(
        summary = "Get wishlist by priority",
        description = "Retrieves wishlist items filtered by priority level"
    )
    public ResponseEntity<List<WishlistResponse>> getWishlistByPriority(
            @Parameter(description = "Priority level (1=Low, 2=Medium, 3=High)", required = true)
            @PathVariable Integer priority,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.debug("Getting wishlist items with priority: {} for user: {}", priority, userDetails.getUsername());
        
        List<WishlistResponse> items = wishlistService.getWishlistByPriority(userDetails.getUsername(), priority);
        return ResponseEntity.ok(items);
    }

    @PostMapping("/{wishlistId}/move-to-cart")
    @Operation(
        summary = "Move wishlist item to cart",
        description = "Moves a wishlist item to the shopping cart"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Item moved successfully"),
        @ApiResponse(responseCode = "404", description = "Wishlist item not found"),
        @ApiResponse(responseCode = "409", description = "Product unavailable or out of stock")
    })
    public ResponseEntity<Map<String, String>> moveToCart(
            @Parameter(description = "Wishlist item ID", required = true)
            @PathVariable Long wishlistId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Moving wishlist item {} to cart for user: {}", wishlistId, userDetails.getUsername());

        wishlistService.moveWishlistItemToCart(userDetails.getUsername(), wishlistId, cartService);
        return ResponseEntity.ok(Map.of("message", "Item moved to cart successfully"));
    }

    @PostMapping("/move-all-to-cart")
    @Operation(
            summary = "Move all wishlist items to cart",
            description = "Moves all available wishlist items to the shopping cart"
    )
    public ResponseEntity<Map<String, String>> moveAllToCart(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("Moving all wishlist items to cart for user: {}", userDetails.getUsername());

        wishlistService.moveAllWishlistToCart(userDetails.getUsername(), cartService);
        return ResponseEntity.ok(Map.of("message", "Wishlist items moved to cart"));
    }

    @GetMapping("/analytics")
    @Operation(
            summary = "Get wishlist analytics",
            description = "Retrieves analytics and statistics about the user's wishlist"
    )
    public ResponseEntity<Map<String, Object>> getWishlistAnalytics(@AuthenticationPrincipal UserDetails userDetails) {
        log.debug("Getting wishlist analytics for user: {}", userDetails.getUsername());

        Map<String, Object> analytics = wishlistService.getWishlistAnalytics(userDetails.getUsername());
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/price-drops")
    @Operation(
            summary = "Get items with price drops",
            description = "Retrieves wishlist items that currently have price discounts"
    )
    public ResponseEntity<List<WishlistResponse>> getItemsWithPriceDrops(@AuthenticationPrincipal UserDetails userDetails) {
        log.debug("Getting wishlist items with price drops for user: {}", userDetails.getUsername());

        List<WishlistResponse> items = wishlistService.getItemsWithPriceDrops(userDetails.getUsername());
        return ResponseEntity.ok(items);
    }

    @GetMapping("/back-in-stock")
    @Operation(
            summary = "Get back in stock items",
            description = "Retrieves wishlist items that are back in stock"
    )
    public ResponseEntity<List<WishlistResponse>> getBackInStockItems(@AuthenticationPrincipal UserDetails userDetails) {
        log.debug("Getting back in stock wishlist items for user: {}", userDetails.getUsername());

        List<WishlistResponse> items = wishlistService.getBackInStockItems(userDetails.getUsername());
        return ResponseEntity.ok(items);
    }

    @GetMapping("/share-url")
    @Operation(
            summary = "Generate wishlist share URL",
            description = "Generates a shareable URL for the user's public wishlist"
    )
    public ResponseEntity<Map<String, String>> generateShareUrl(@AuthenticationPrincipal UserDetails userDetails) {
        log.debug("Generating wishlist share URL for user: {}", userDetails.getUsername());

        String shareUrl = wishlistService.generateWishlistShareUrl(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("shareUrl", shareUrl));
    }

    // Public endpoints for wishlist sharing

    @GetMapping("/shared/{userId}")
    @Operation(
            summary = "Get shared wishlist",
            description = "Retrieves public wishlist items for a specific user (no authentication required)"
    )
    public ResponseEntity<List<WishlistResponse>> getSharedWishlist(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long userId) {

        log.debug("Getting shared wishlist for user ID: {}", userId);

        List<WishlistResponse> sharedItems = wishlistService.getSharedWishlist(userId);
        return ResponseEntity.ok(sharedItems);
    }
}