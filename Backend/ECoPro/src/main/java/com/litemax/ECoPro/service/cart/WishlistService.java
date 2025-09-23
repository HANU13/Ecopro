package com.litemax.ECoPro.service.cart;

import com.litemax.ECoPro.dto.cart.CartRequest;
import com.litemax.ECoPro.dto.cart.WishlistRequest;
import com.litemax.ECoPro.dto.cart.WishlistResponse;
import com.litemax.ECoPro.entity.auth.User;
import com.litemax.ECoPro.entity.cart.Wishlist;
import com.litemax.ECoPro.entity.product.Product;
import com.litemax.ECoPro.entity.product.ProductVariant;
import com.litemax.ECoPro.exception.ResourceNotFoundException;
import com.litemax.ECoPro.exception.ValidationException;
import com.litemax.ECoPro.repository.auth.UserRepository;
import com.litemax.ECoPro.repository.cart.WishlistRepository;
import com.litemax.ECoPro.repository.product.ProductRepository;
import com.litemax.ECoPro.repository.product.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    // Wishlist management methods

    public List<WishlistResponse> getUserWishlist(String userEmail) {
        log.debug("Getting wishlist for user: {}", userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        List<Wishlist> wishlistItems = wishlistRepository.findByUserIdOrderByPriorityDescCreatedAtDesc(user.getId());

        log.debug("Found {} wishlist items for user: {}", wishlistItems.size(), userEmail);
        return wishlistItems.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public Page<WishlistResponse> getUserWishlistPaginated(String userEmail, int page, int size, String sortBy, String sortDirection) {
        log.debug("Getting paginated wishlist for user: {} - page: {}, size: {}", userEmail, page, size);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Wishlist> wishlistPage = wishlistRepository.findByUserId(user.getId(), pageable);

        log.debug("Found {} wishlist items out of {} total for user: {}", 
                wishlistPage.getNumberOfElements(), wishlistPage.getTotalElements(), userEmail);

        return wishlistPage.map(this::convertToResponse);
    }

    public WishlistResponse addToWishlist(String userEmail, WishlistRequest request) {
        log.info("Adding item to wishlist for user: {} - Product: {}", userEmail, request.getProductId());

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        // Validate and get product
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.getProductId()));

        if (product.getStatus() != Product.ProductStatus.ACTIVE) {
            throw new ValidationException("Product is not available");
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

        // Check if item already exists in wishlist
        boolean alreadyExists;
        if (variant != null) {
            alreadyExists = wishlistRepository.existsByUserIdAndProductIdAndVariantId(
                    user.getId(), product.getId(), variant.getId());
        } else {
            alreadyExists = wishlistRepository.existsByUserIdAndProductId(user.getId(), product.getId());
        }

        if (alreadyExists) {
            throw new ValidationException("Item is already in your wishlist");
        }

        // Create wishlist item
        Wishlist wishlistItem = new Wishlist();
        wishlistItem.setUser(user);
        wishlistItem.setProduct(product);
        wishlistItem.setVariant(variant);
        wishlistItem.setNotes(request.getNotes());
        wishlistItem.setIsPublic(request.getIsPublic());
        wishlistItem.setPriority(request.getPriority());

        wishlistItem = wishlistRepository.save(wishlistItem);
        log.info("Item added to wishlist successfully with ID: {}", wishlistItem.getId());

        return convertToResponse(wishlistItem);
    }

    public WishlistResponse updateWishlistItem(String userEmail, Long wishlistId, WishlistRequest request) {
        log.info("Updating wishlist item: {} for user: {}", wishlistId, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        Wishlist wishlistItem = wishlistRepository.findById(wishlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist item not found: " + wishlistId));

        // Verify ownership
        if (!wishlistItem.getUser().getId().equals(user.getId())) {
            throw new ValidationException("Wishlist item does not belong to the user");
        }

        // Update fields
        wishlistItem.setNotes(request.getNotes());
        wishlistItem.setIsPublic(request.getIsPublic());
        wishlistItem.setPriority(request.getPriority());

        wishlistItem = wishlistRepository.save(wishlistItem);
        log.info("Wishlist item updated successfully");

        return convertToResponse(wishlistItem);
    }

    public void removeFromWishlist(String userEmail, Long wishlistId) {
        log.info("Removing item from wishlist: {} for user: {}", wishlistId, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        Wishlist wishlistItem = wishlistRepository.findById(wishlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist item not found: " + wishlistId));

        // Verify ownership
        if (!wishlistItem.getUser().getId().equals(user.getId())) {
            throw new ValidationException("Wishlist item does not belong to the user");
        }

        wishlistRepository.delete(wishlistItem);
        log.info("Item removed from wishlist successfully");
    }

    public void removeFromWishlistByProduct(String userEmail, Long productId, Long variantId) {
        log.info("Removing product from wishlist: {} (variant: {}) for user: {}", productId, variantId, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        if (variantId != null) {
            wishlistRepository.deleteByUserIdAndProductIdAndVariantId(user.getId(), productId, variantId);
        } else {
            wishlistRepository.deleteByUserIdAndProductId(user.getId(), productId);
        }

        log.info("Product removed from wishlist successfully");
    }

    public void clearWishlist(String userEmail) {
        log.info("Clearing wishlist for user: {}", userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        wishlistRepository.deleteByUserId(user.getId());
        log.info("Wishlist cleared successfully for user: {}", userEmail);
    }

    // Wishlist query methods

    public List<WishlistResponse> getWishlistByPriority(String userEmail, Integer priority) {
        log.debug("Getting wishlist items with priority: {} for user: {}", priority, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        List<Wishlist> wishlistItems = wishlistRepository.findByUserIdAndPriority(user.getId(), priority);

        log.debug("Found {} wishlist items with priority: {} for user: {}", wishlistItems.size(), priority, userEmail);
        return wishlistItems.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<WishlistResponse> getPublicWishlist(String userEmail) {
        log.debug("Getting public wishlist for user: {}", userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        List<Wishlist> publicItems = wishlistRepository.findByUserIdAndIsPublicTrue(user.getId());

        log.debug("Found {} public wishlist items for user: {}", publicItems.size(), userEmail);
        return publicItems.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public boolean isInWishlist(String userEmail, Long productId, Long variantId) {
        log.debug("Checking if product {} (variant: {}) is in wishlist for user: {}", productId, variantId, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        boolean exists;
        if (variantId != null) {
            exists = wishlistRepository.existsByUserIdAndProductIdAndVariantId(user.getId(), productId, variantId);
        } else {
            exists = wishlistRepository.existsByUserIdAndProductId(user.getId(), productId);
        }

        log.debug("Product {} (variant: {}) is {} in wishlist for user: {}",
                productId, variantId, exists ? "present" : "not present", userEmail);
        return exists;
    }

    public long getWishlistCount(String userEmail) {
        log.debug("Getting wishlist count for user: {}", userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        long count = wishlistRepository.countByUserId(user.getId());
        log.debug("Wishlist count for user {}: {}", userEmail, count);
        return count;
    }

    // Wishlist and Cart integration

    public void moveWishlistItemToCart(String userEmail, Long wishlistId, CartService cartService) {
        log.info("Moving wishlist item {} to cart for user: {}", wishlistId, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        Wishlist wishlistItem = wishlistRepository.findById(wishlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist item not found: " + wishlistId));

        // Verify ownership
        if (!wishlistItem.getUser().getId().equals(user.getId())) {
            throw new ValidationException("Wishlist item does not belong to the user");
        }

        // Check if product is still available
        if (!wishlistItem.isAvailable()) {
            throw new ValidationException("Product is no longer available");
        }

        // Create cart request
        CartRequest cartRequest = new CartRequest();
        cartRequest.setProductId(wishlistItem.getProduct().getId());
        cartRequest.setVariantId(wishlistItem.getVariant() != null ? wishlistItem.getVariant().getId() : null);
        cartRequest.setQuantity(1);

        // Add to cart
        cartService.addItemToCart(userEmail, cartRequest);

        // Remove from wishlist
        wishlistRepository.delete(wishlistItem);

        log.info("Successfully moved wishlist item to cart and removed from wishlist");
    }

    public void moveAllWishlistToCart(String userEmail, CartService cartService) {
        log.info("Moving all wishlist items to cart for user: {}", userEmail);

        List<WishlistResponse> wishlistItems = getUserWishlist(userEmail);
        int movedCount = 0;
        int skippedCount = 0;

        for (WishlistResponse item : wishlistItems) {
            try {
                if (item.isAvailable() && item.isCanAddToCart()) {
                    moveWishlistItemToCart(userEmail, item.getId(), cartService);
                    movedCount++;
                } else {
                    skippedCount++;
                    log.debug("Skipped wishlist item: {} (not available or cannot add to cart)", item.getDisplayName());
                }
            } catch (Exception e) {
                skippedCount++;
                log.warn("Failed to move wishlist item {} to cart: {}", item.getDisplayName(), e.getMessage());
            }
        }

        log.info("Moved {} items to cart, skipped {} items for user: {}", movedCount, skippedCount, userEmail);
    }

    // Wishlist analytics and statistics

    public Map<String, Object> getWishlistAnalytics(String userEmail) {
        log.debug("Getting wishlist analytics for user: {}", userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        List<Wishlist> wishlistItems = wishlistRepository.findByUserId(user.getId());

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("totalItems", wishlistItems.size());
        analytics.put("publicItems", wishlistItems.stream().filter(Wishlist::getIsPublic).count());
        analytics.put("privateItems", wishlistItems.stream().filter(item -> !item.getIsPublic()).count());

        // Priority distribution
        Map<String, Long> priorityDistribution = wishlistItems.stream()
                .collect(Collectors.groupingBy(item -> item.getPriorityText(), Collectors.counting()));
        analytics.put("priorityDistribution", priorityDistribution);

        // Availability status
        long availableItems = wishlistItems.stream().filter(Wishlist::isAvailable).count();
        long unavailableItems = wishlistItems.size() - availableItems;
        analytics.put("availableItems", availableItems);
        analytics.put("unavailableItems", unavailableItems);

        // Stock status
        long inStockItems = wishlistItems.stream().filter(Wishlist::isInStock).count();
        long outOfStockItems = wishlistItems.size() - inStockItems;
        analytics.put("inStockItems", inStockItems);
        analytics.put("outOfStockItems", outOfStockItems);

        log.debug("Generated wishlist analytics for user: {}", userEmail);
        return analytics;
    }

    // Wishlist sharing and social features

    public List<WishlistResponse> getSharedWishlist(Long userId) {
        log.debug("Getting shared wishlist for user ID: {}", userId);

        List<Wishlist> publicItems = wishlistRepository.findByUserIdAndIsPublicTrue(userId);

        log.debug("Found {} shared wishlist items for user ID: {}", publicItems.size(), userId);
        return publicItems.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public String generateWishlistShareUrl(String userEmail) {
        log.debug("Generating wishlist share URL for user: {}", userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        // Generate a shareable URL - in a real implementation, you might use a UUID or encoded user ID
        String shareUrl = "/wishlist/shared/" + user.getId();

        log.debug("Generated share URL: {} for user: {}", shareUrl, userEmail);
        return shareUrl;
    }

    // Price monitoring and notifications

    public List<WishlistResponse> getItemsWithPriceDrops(String userEmail) {
        log.debug("Getting wishlist items with price drops for user: {}", userEmail);

        List<WishlistResponse> wishlistItems = getUserWishlist(userEmail);

        // Filter items with price drops (compare price > current price)
        List<WishlistResponse> itemsWithPriceDrops = wishlistItems.stream()
                .filter(WishlistResponse::isHasDiscount)
                .collect(Collectors.toList());

        log.debug("Found {} items with price drops for user: {}", itemsWithPriceDrops.size(), userEmail);
        return itemsWithPriceDrops;
    }

    public List<WishlistResponse> getBackInStockItems(String userEmail) {
        log.debug("Getting back in stock wishlist items for user: {}", userEmail);

        List<WishlistResponse> wishlistItems = getUserWishlist(userEmail);

        // This would typically involve checking against a previous out-of-stock status
        // For now, we'll return currently in-stock items
        List<WishlistResponse> backInStockItems = wishlistItems.stream()
                .filter(WishlistResponse::isInStock)
                .collect(Collectors.toList());

        log.debug("Found {} back in stock items for user: {}", backInStockItems.size(), userEmail);
        return backInStockItems;
    }

    // Private helper methods

    private WishlistResponse convertToResponse(Wishlist wishlist) {
        Product product = wishlist.getProduct();
        ProductVariant variant = wishlist.getVariant();

        BigDecimal price = variant != null ? variant.getPrice() : product.getPrice();
        BigDecimal comparePrice = variant != null ? variant.getComparePrice() : product.getComparePrice();

        BigDecimal discountPercentage = BigDecimal.ZERO;
        boolean hasDiscount = false;

        if (comparePrice != null && comparePrice.compareTo(price) > 0) {
            hasDiscount = true;
            discountPercentage = comparePrice.subtract(price)
                    .divide(comparePrice, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        boolean canAddToCart = wishlist.isAvailable() &&
                (wishlist.isInStock() ||
                        (variant != null && variant.getInventoryPolicy() == ProductVariant.InventoryPolicy.CONTINUE) ||
                        product.isAllowBackorders());

        return WishlistResponse.builder()
                .id(wishlist.getId())
                .userId(wishlist.getUser().getId())
                .productId(product.getId())
                .productName(product.getName())
                .productSlug(product.getSlug())
                .variantId(variant != null ? variant.getId() : null)
                .variantName(variant != null ? variant.getName() : null)
                .displayName(wishlist.getDisplayName())
                .notes(wishlist.getNotes())
                .isPublic(wishlist.getIsPublic())
                .priority(wishlist.getPriority())
                .priorityText(wishlist.getPriorityText())
                .createdAt(wishlist.getCreatedAt())
                .updatedAt(wishlist.getUpdatedAt())
                .price(price)
                .comparePrice(comparePrice)
                .imageUrl(wishlist.getImageUrl())
                .inStock(wishlist.isInStock())
                .available(wishlist.isAvailable())
                .stockStatus(getWishlistStockStatus(wishlist))
                .productUrl("/products/" + product.getSlug())
                .formattedPrice(formatPrice(price))
                .formattedComparePrice(comparePrice != null ? formatPrice(comparePrice) : null)
                .hasDiscount(hasDiscount)
                .discountPercentage(discountPercentage)
                .canAddToCart(canAddToCart)
                .build();
    }

    private String getWishlistStockStatus(Wishlist wishlist) {
        if (!wishlist.isAvailable()) return "UNAVAILABLE";
        if (!wishlist.isInStock()) return "OUT_OF_STOCK";
        return "IN_STOCK";
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) return "$0.00";
        return String.format("$%.2f", price);
    }
}
