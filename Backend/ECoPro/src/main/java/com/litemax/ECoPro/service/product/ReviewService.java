package com.litemax.ECoPro.service.product;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.litemax.ECoPro.dto.product.ProductRatingResponse;
import com.litemax.ECoPro.dto.product.ReviewCreateRequest;
import com.litemax.ECoPro.dto.product.ReviewModerationRequest;
import com.litemax.ECoPro.dto.product.ReviewResponse;
import com.litemax.ECoPro.dto.product.ReviewUpdateRequest;
import com.litemax.ECoPro.dto.product.SellerResponseRequest;
import com.litemax.ECoPro.entity.auth.User;
import com.litemax.ECoPro.entity.order.Order;
import com.litemax.ECoPro.entity.product.Product;
import com.litemax.ECoPro.entity.product.ProductRating;
import com.litemax.ECoPro.entity.product.Review;
import com.litemax.ECoPro.entity.product.ReviewHelpful;
import com.litemax.ECoPro.entity.product.ReviewImage;
import com.litemax.ECoPro.exception.ResourceNotFoundException;
import com.litemax.ECoPro.exception.ValidationException;
import com.litemax.ECoPro.repository.order.OrderRepository;
import com.litemax.ECoPro.repository.product.ProductRatingRepository;
import com.litemax.ECoPro.repository.product.ProductRepository;
import com.litemax.ECoPro.repository.product.ReviewHelpfulRepository;
import com.litemax.ECoPro.repository.product.ReviewRepository;
import com.litemax.ECoPro.service.UserService;
import com.litemax.ECoPro.service.file.FileUploadService;
import com.litemax.ECoPro.service.order.OrderService;
import com.litemax.ECoPro.util.MapperUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {
    
    private final ReviewRepository reviewRepository;
    private final ProductRatingRepository productRatingRepository;
    private final ReviewHelpfulRepository reviewHelpfulRepository;
    private final UserService userService;
    private final ProductRepository productService;
    private final OrderRepository orderService;
    private final FileUploadService fileUploadService;
    private final MapperUtil mapperUtil;
    
    @Transactional
    public ReviewResponse createReview(ReviewCreateRequest request, Long userId) {
        log.info("Creating review for product: {} by user: {}", request.getProductId(), userId);
        
        try {
            // Get user, product, and order
            User user = userService.findById(userId);
            Product product = productService.findById(request.getProductId())
            		.orElseThrow(() -> {
                        return new ResourceNotFoundException("product not found with ID: " + request.getProductId());
                    });
            Order order = orderService.findById(request.getOrderId())
            		.orElseThrow(() -> {
                        return new ResourceNotFoundException("Order not found with ID: " + request.getOrderId());
                    });
            
            // Validate user owns the order
            if (!order.getUser().getId().equals(userId)) {
                log.error("User {} trying to review with unauthorized order {}", userId, request.getOrderId());
                throw new ValidationException("Unauthorized access to order");
            }
            
            // Validate order contains the product
            boolean orderContainsProduct = order.getOrderItems().stream()
                .anyMatch(item -> item.getProduct().getId().equals(request.getProductId()));
            
            if (!orderContainsProduct) {
                log.error("Order {} does not contain product {}", request.getOrderId(), request.getProductId());
                throw new ValidationException("You can only review products you have purchased");
            }
            
            // Validate order is delivered
            if (order.getStatus() != Order.OrderStatus.DELIVERED) {
                log.error("Cannot review product from undelivered order: {}", order.getStatus());
                throw new ValidationException("You can only review products from delivered orders");
            }
            
            // Check if user already reviewed this product for this order
            if (reviewRepository.findByUserAndProductAndOrder(user, product, order).isPresent()) {
                log.error("User {} already reviewed product {} for order {}", userId, request.getProductId(), request.getOrderId());
                throw new ValidationException("You have already reviewed this product for this order");
            }
            
            // Create review
            Review review = new Review();
            review.setUser(user);
            review.setProduct(product);
            review.setOrder(order);
            review.setRating(request.getRating());
            review.setTitle(request.getTitle());
            review.setReviewText(request.getReviewText());
            review.setIsAnonymous(request.getIsAnonymous() != null ? request.getIsAnonymous() : false);
            review.setStatus(Review.ReviewStatus.PENDING); // Reviews need approval
            
            review = reviewRepository.save(review);
            log.info("Review created with ID: {} for product: {}", review.getId(), request.getProductId());
            
            // Handle image uploads
            if (request.getImages() != null && !request.getImages().isEmpty()) {
                List<ReviewImage> reviewImages = uploadReviewImages(request.getImages(), review);
                review.setReviewImages(reviewImages);
            }
            
            // Update product rating (even pending reviews affect internal calculations)
            updateProductRating(product, request.getRating(), true);
            
            ReviewResponse response = mapperUtil.mapToReviewResponse(review, userId);
            log.info("Review created successfully: {}", review.getId());
            return response;
            
        } catch (Exception e) {
            log.error("Error creating review: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    @Transactional
    public ReviewResponse updateReview(Long reviewId, ReviewUpdateRequest request, Long userId) {
        log.info("Updating review ID: {} by user: {}", reviewId, userId);
        
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> {
                log.error("Review not found with ID: {}", reviewId);
                return new ResourceNotFoundException("Review not found with ID: " + reviewId);
            });
        
        // Validate user owns the review
        if (!review.getUser().getId().equals(userId)) {
            log.error("User {} trying to update unauthorized review {}", userId, reviewId);
            throw new ValidationException("You can only update your own reviews");
        }
        
        // Store old rating for product rating update
        Integer oldRating = review.getRating();
        
        // Update fields if provided
        if (request.getRating() != null && !request.getRating().equals(oldRating)) {
            review.setRating(request.getRating());
            // Update product rating
            updateProductRating(review.getProduct(), oldRating, false); // Remove old rating
            updateProductRating(review.getProduct(), request.getRating(), true); // Add new rating
        }
        
        if (request.getTitle() != null) {
            review.setTitle(request.getTitle());
        }
        
        if (request.getReviewText() != null) {
            review.setReviewText(request.getReviewText());
        }
        
        if (request.getIsAnonymous() != null) {
            review.setIsAnonymous(request.getIsAnonymous());
        }
        
        // Reset status to pending if content changed
        if (request.getRating() != null || request.getReviewText() != null) {
            review.setStatus(Review.ReviewStatus.PENDING);
        }
        
        review = reviewRepository.save(review);
        log.info("Review updated successfully: {}", reviewId);
        
        return mapperUtil.mapToReviewResponse(review, userId);
    }
    
    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        log.info("Deleting review ID: {} by user: {}", reviewId, userId);
        
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> {
                log.error("Review not found with ID: {}", reviewId);
                return new ResourceNotFoundException("Review not found with ID: " + reviewId);
            });
        
        // Validate user owns the review or is admin
        if (!review.getUser().getId().equals(userId)) {
            // Check if user is admin (this would be handled by security, but adding check)
            log.error("User {} trying to delete unauthorized review {}", userId, reviewId);
            throw new ValidationException("You can only delete your own reviews");
        }
        
        // Update product rating (remove this review's rating)
        updateProductRating(review.getProduct(), review.getRating(), false);
        
        reviewRepository.delete(review);
        log.info("Review deleted successfully: {}", reviewId);
    }
    
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getProductReviews(Long productId, Pageable pageable, Long userId) {
        log.info("Fetching reviews for product: {}, page: {}, size: {}", 
                productId, pageable.getPageNumber(), pageable.getPageSize());
        
        Product product = productService.findById(productId)
        		.orElseThrow(() -> {
                    return new ResourceNotFoundException("product not found with ID: " + productId);
                });;
        
        // Only show approved reviews to customers
        List<Review.ReviewStatus> allowedStatuses = List.of(Review.ReviewStatus.APPROVED);
        
        Page<Review> reviews = reviewRepository.findByProductAndStatusOrderByHelpfulness(
            product, allowedStatuses, pageable);
        
        log.debug("Found {} reviews for product: {}", reviews.getTotalElements(), productId);
        
        return reviews.map(review -> mapperUtil.mapToReviewResponse(review, userId));
    }
    
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getUserReviews(Long userId, Pageable pageable) {
        log.info("Fetching reviews for user: {}", userId);
        
        User user = userService.findById(userId);
        Page<Review> reviews = reviewRepository.findByUser(user, pageable);
        
        log.debug("Found {} reviews for user: {}", reviews.getTotalElements(), userId);
        
        return reviews.map(review -> mapperUtil.mapToReviewResponse(review, userId));
    }
    
    @Transactional
    public void markReviewHelpful(Long reviewId, Boolean isHelpful, Long userId) {
        log.info("User {} marking review {} as helpful: {}", userId, reviewId, isHelpful);
        
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> {
                log.error("Review not found with ID: {}", reviewId);
                return new ResourceNotFoundException("Review not found with ID: " + reviewId);
            });
        
        User user = userService.findById(userId);
        
        // Check if user already voted
        ReviewHelpful existingVote = reviewHelpfulRepository.findByUserAndReview(user, review)
            .orElse(null);
        
        if (existingVote != null) {
            // Update existing vote
            if (existingVote.getIsHelpful().equals(isHelpful)) {
                log.debug("User {} already marked review {} as helpful: {}", userId, reviewId, isHelpful);
                return; // No change needed
            }
            
            // Update counters for the change
            if (existingVote.getIsHelpful()) {
                review.setHelpfulCount(review.getHelpfulCount() - 1);
                review.setUnhelpfulCount(review.getUnhelpfulCount() + 1);
            } else {
                review.setUnhelpfulCount(review.getUnhelpfulCount() - 1);
                review.setHelpfulCount(review.getHelpfulCount() + 1);
            }
            
            existingVote.setIsHelpful(isHelpful);
            reviewHelpfulRepository.save(existingVote);
        } else {
            // Create new vote
            ReviewHelpful newVote = new ReviewHelpful();
            newVote.setUser(user);
            newVote.setReview(review);
            newVote.setIsHelpful(isHelpful);
            reviewHelpfulRepository.save(newVote);
            
            // Update counters
            if (isHelpful) {
                review.setHelpfulCount(review.getHelpfulCount() + 1);
            } else {
                review.setUnhelpfulCount(review.getUnhelpfulCount() + 1);
            }
        }
        
        reviewRepository.save(review);
        log.info("Review helpfulness updated: {} helpful votes, {} unhelpful votes", 
                review.getHelpfulCount(), review.getUnhelpfulCount());
    }
    
    @Transactional(readOnly = true)
    public ProductRatingResponse getProductRating(Long productId) {
        log.info("Fetching product rating for product: {}", productId);

        Product product = productService.findById(productId)
        		.orElseThrow(() -> {
                    return new ResourceNotFoundException("product not found with ID: " + productId);
                });;
        ProductRating rating = productRatingRepository.findByProduct(product)
            .orElse(createEmptyProductRating(product));
        
        return mapperUtil.mapToProductRatingResponse(rating);
    }
    
    // Admin methods
    @Transactional
    public ReviewResponse moderateReview(Long reviewId, ReviewModerationRequest request, Long moderatorId) {
        log.info("Moderating review ID: {} by moderator: {}, new status: {}", 
                reviewId, moderatorId, request.getStatus());
        
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> {
                log.error("Review not found with ID: {}", reviewId);
                return new ResourceNotFoundException("Review not found with ID: " + reviewId);
            });
        
        Review.ReviewStatus oldStatus = review.getStatus();
        review.setStatus(request.getStatus());
        review.setModeratorNotes(request.getModeratorNotes());
        review.setModeratedBy(moderatorId);
        review.setModeratedAt(LocalDateTime.now());
        
        review = reviewRepository.save(review);
        
        log.info("Review moderated: {} status changed from {} to {}", 
                reviewId, oldStatus, request.getStatus());
        
        return mapperUtil.mapToReviewResponse(review, moderatorId);
    }
    
    @Transactional
    public ReviewResponse addSellerResponse(Long reviewId, SellerResponseRequest request, Long sellerId) {
        log.info("Adding seller response to review ID: {} by seller: {}", reviewId, sellerId);
        
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> {
                log.error("Review not found with ID: {}", reviewId);
                return new ResourceNotFoundException("Review not found with ID: " + reviewId);
            });
        
        review.setSellerResponse(request.getResponse());
        review.setSellerResponseBy(sellerId);
        review.setSellerResponseAt(LocalDateTime.now());
        
        review = reviewRepository.save(review);
        log.info("Seller response added to review: {}", reviewId);
        
        return mapperUtil.mapToReviewResponse(review, sellerId);
    }
    
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsForModeration(Pageable pageable) {
        log.info("Fetching reviews needing moderation");
        
        Page<Review> reviews = reviewRepository.findReviewsNeedingModeration(3, pageable);
        log.debug("Found {} reviews needing moderation", reviews.getTotalElements());
        
        return reviews.map(review -> mapperUtil.mapToReviewResponse(review, null));
    }
    
    private void updateProductRating(Product product, Integer rating, boolean isAdd) {
        ProductRating productRating = productRatingRepository.findByProduct(product)
            .orElse(createEmptyProductRating(product));
        
        productRating.updateRatingCounts(rating, isAdd);
        productRatingRepository.save(productRating);
        
        log.debug("Updated product rating for product {}: average = {}, total = {}",
                product.getId(), productRating.getAverageRating(), productRating.getTotalReviews());
    }

    private ProductRating createEmptyProductRating(Product product) {
        ProductRating rating = new ProductRating();
        rating.setProduct(product);
        return rating;
    }

    private List<ReviewImage> uploadReviewImages(List<MultipartFile> images, Review review) {
        List<ReviewImage> reviewImages = new ArrayList<>();

        for (int i = 0; i < images.size() && i < 5; i++) { // Max 5 images
            MultipartFile image = images.get(i);

            try {
                String imageUrl = fileUploadService.uploadFile(image, "reviews");

                ReviewImage reviewImage = new ReviewImage();
                reviewImage.setReview(review);
                reviewImage.setImageUrl(imageUrl);
                reviewImage.setDisplayOrder(i);
                reviewImage.setAltText("Review image " + (i + 1));

                reviewImages.add(reviewImage);
                log.debug("Uploaded review image: {}", imageUrl);

            } catch (Exception e) {
                log.error("Failed to upload review image: {}", e.getMessage());
                // Continue with other images
            }
        }

        return reviewImages;
    }
}

    