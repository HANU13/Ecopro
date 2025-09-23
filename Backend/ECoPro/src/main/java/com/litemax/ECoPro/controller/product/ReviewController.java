package com.litemax.ECoPro.controller.product;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.litemax.ECoPro.dto.product.ProductRatingResponse;
import com.litemax.ECoPro.dto.product.ReviewCreateRequest;
import com.litemax.ECoPro.dto.product.ReviewModerationRequest;
import com.litemax.ECoPro.dto.product.ReviewResponse;
import com.litemax.ECoPro.dto.product.ReviewUpdateRequest;
import com.litemax.ECoPro.dto.product.SellerResponseRequest;
import com.litemax.ECoPro.service.product.ReviewService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {
    
    private final ReviewService reviewService;
    
    // Customer APIs
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ReviewResponse> createReview(@Valid @ModelAttribute ReviewCreateRequest request,
                                                       Authentication authentication) {
        log.info("Creating review for product: {} by user: {}", request.getProductId(), authentication.getName());
        
        Long userId = getUserIdFromAuthentication(authentication);
        ReviewResponse review = reviewService.createReview(request, userId);
        
        log.info("Review created successfully: {}", review.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ReviewResponse> updateReview(@PathVariable Long id,
                                                     @Valid @RequestBody ReviewUpdateRequest request,
                                                     Authentication authentication) {
        log.info("Updating review ID: {} by user: {}", id, authentication.getName());
        
        Long userId = getUserIdFromAuthentication(authentication);
        ReviewResponse review = reviewService.updateReview(id, request, userId);
        
        log.info("Review updated successfully: {}", id);
        return ResponseEntity.ok(review);
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id,
                                           Authentication authentication) {
        log.info("Deleting review ID: {} by user: {}", id, authentication.getName());
        
        Long userId = getUserIdFromAuthentication(authentication);
        reviewService.deleteReview(id, userId);
        
        log.info("Review deleted successfully: {}", id);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/my-reviews")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Page<ReviewResponse>> getMyReviews(@RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "10") int size,
                                                           @RequestParam(defaultValue = "createdAt") String sortBy,
                                                           @RequestParam(defaultValue = "desc") String sortDir,
                                                           Authentication authentication) {
        log.info("Fetching reviews for user: {}", authentication.getName());
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Long userId = getUserIdFromAuthentication(authentication);
        Page<ReviewResponse> reviews = reviewService.getUserReviews(userId, pageable);
        
        return ResponseEntity.ok(reviews);
    }
    
    @PostMapping("/{id}/helpful")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> markReviewHelpful(@PathVariable Long id,
                                                @RequestParam Boolean isHelpful,
                                                Authentication authentication) {
        log.info("User {} marking review {} as helpful: {}", authentication.getName(), id, isHelpful);
        
        Long userId = getUserIdFromAuthentication(authentication);
        reviewService.markReviewHelpful(id, isHelpful, userId);
        
        return ResponseEntity.ok().build();
    }
    
    // Public APIs (no authentication required)
    @GetMapping("/product/{productId}")
    public ResponseEntity<Page<ReviewResponse>> getProductReviews(@PathVariable Long productId,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "10") int size,
                                                                @RequestParam(defaultValue = "helpfulCount") String sortBy,
                                                                @RequestParam(defaultValue = "desc") String sortDir,
                                                                Authentication authentication) {
        log.info("Fetching reviews for product: {}", productId);
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Long userId = authentication != null ? getUserIdFromAuthentication(authentication) : null;
        Page<ReviewResponse> reviews = reviewService.getProductReviews(productId, pageable, userId);
        
        return ResponseEntity.ok(reviews);
    }
    
    @GetMapping("/product/{productId}/rating")
    public ResponseEntity<ProductRatingResponse> getProductRating(@PathVariable Long productId) {
        log.info("Fetching rating for product: {}", productId);
        
        ProductRatingResponse rating = reviewService.getProductRating(productId);
        return ResponseEntity.ok(rating);
    }
    
    // Admin APIs
    @GetMapping("/admin/moderation")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ReviewResponse>> getReviewsForModeration(@RequestParam(defaultValue = "0") int page,
                                                                      @RequestParam(defaultValue = "20") int size) {
        log.info("Admin fetching reviews for moderation");
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        Page<ReviewResponse> reviews = reviewService.getReviewsForModeration(pageable);
        
        return ResponseEntity.ok(reviews);
    }
    
    @PutMapping("/admin/{id}/moderate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReviewResponse> moderateReview(@PathVariable Long id,
                                                       @Valid @RequestBody ReviewModerationRequest request,
                                                       Authentication authentication) {
        log.info("Admin moderating review ID: {} with status: {}", id, request.getStatus());
        
        Long moderatorId = getUserIdFromAuthentication(authentication);
        ReviewResponse review = reviewService.moderateReview(id, request, moderatorId);
        
        log.info("Review moderated successfully: {}", id);
        return ResponseEntity.ok(review);
    }
    
    // Seller APIs
    @PostMapping("/seller/{id}/respond")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<ReviewResponse> addSellerResponse(@PathVariable Long id,
                                                          @Valid @RequestBody SellerResponseRequest request,
                                                          Authentication authentication) {
        log.info("Adding seller response to review ID: {}", id);
        
        Long sellerId = getUserIdFromAuthentication(authentication);
        ReviewResponse review = reviewService.addSellerResponse(id, request, sellerId);
        
        log.info("Seller response added successfully to review: {}", id);
        return ResponseEntity.ok(review);
    }
    
    private Long getUserIdFromAuthentication(Authentication authentication) {
        return Long.parseLong(authentication.getName()); // Adjust as needed
    }
}