package com.litemax.ECoPro.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "app.cart")
@Data
public class CartConfig {

    private Duration cartExpiration = Duration.ofDays(30);
    private Duration abandonedCartThreshold = Duration.ofDays(3);
    private int maxItemsPerCart = 100;
    private BigDecimal freeShippingThreshold = new BigDecimal("75.00");
    private BigDecimal taxRate = new BigDecimal("0.08"); // 8%
    private boolean allowBackorders = true;
    private boolean autoMergeGuestCart = true;
    
    // Wishlist settings
    private int maxWishlistItems = 500;
    private boolean allowPublicWishlists = true;
    private boolean enableWishlistSharing = true;
    private boolean enablePriceDropNotifications = true;
    private boolean enableBackInStockNotifications = true;
}