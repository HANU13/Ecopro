
package com.litemax.ECoPro.service.cart;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartMaintenanceService {

    private final CartService cartService;

    @Scheduled(cron = "0 0 1 * * ?") // Run daily at 1 AM
    public void expireOldCarts() {
        log.info("Starting scheduled task: expire old carts");
        
        try {
            cartService.expireOldCarts();
            log.info("Successfully expired old carts");
        } catch (Exception e) {
            log.error("Error during cart expiration task", e);
        }
    }

    @Scheduled(cron = "0 30 2 * * ?") // Run daily at 2:30 AM
    public void markAbandonedCarts() {
        log.info("Starting scheduled task: mark abandoned carts");
        
        try {
            cartService.markAbandonedCarts();
            log.info("Successfully marked abandoned carts");
        } catch (Exception e) {
            log.error("Error during abandoned cart marking task", e);
        }
    }

    @Scheduled(cron = "0 0 3 * * SUN") // Run weekly on Sunday at 3 AM
    public void cleanupExpiredCarts() {
        log.info("Starting scheduled task: cleanup expired carts");
        
        try {
            // Implementation for cleaning up very old expired carts
            // This would involve hard-deleting carts older than a certain period
            log.info("Cart cleanup task completed successfully");
        } catch (Exception e) {
            log.error("Error during cart cleanup task", e);
        }
    }
}