package com.litemax.ECoPro.util;

import com.litemax.ECoPro.dto.inventory.InventoryResponse;
import com.litemax.ECoPro.dto.order.*;
import com.litemax.ECoPro.dto.product.ProductRatingResponse;
import com.litemax.ECoPro.dto.product.ReviewImageResponse;
import com.litemax.ECoPro.dto.product.ReviewResponse;
import com.litemax.ECoPro.entity.inventory.Inventory;
import com.litemax.ECoPro.entity.order.*;

import java.util.stream.Collectors;

import com.litemax.ECoPro.entity.product.ProductRating;
import com.litemax.ECoPro.entity.product.Review;
import com.litemax.ECoPro.entity.product.ReviewImage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;


@Component
public class MapperUtil {
    public OrderResponse mapToOrderResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setStatus(order.getStatus());
        response.setSubtotal(order.getSubtotal());
        response.setTaxAmount(order.getTaxAmount());
        response.setShippingAmount(order.getShippingAmount());
        response.setDiscountAmount(order.getDiscountAmount());
        response.setTotalAmount(order.getTotalAmount());
        response.setShippingAddressLine1(order.getShippingAddressLine1());
        response.setShippingAddressLine2(order.getShippingAddressLine2());
        response.setShippingCity(order.getShippingCity());
        response.setShippingState(order.getShippingState());
        response.setShippingPostalCode(order.getShippingPostalCode());
        response.setShippingCountry(order.getShippingCountry());
        response.setShippingPhone(order.getShippingPhone());
        response.setNotes(order.getNotes());
        response.setEstimatedDeliveryDate(order.getEstimatedDeliveryDate());
        response.setActualDeliveryDate(order.getActualDeliveryDate());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());

        if (order.getOrderItems() != null) {
            response.setOrderItems(
                    order.getOrderItems().stream()
                            .map(this::mapToOrderItemResponse)
                            .collect(Collectors.toList())
            );
        }

        if (order.getPayments() != null) {
            response.setPayments(
                    order.getPayments().stream()
                            .map(this::mapToPaymentResponse)
                            .collect(Collectors.toList())
            );
        }

        if (order.getShipments() != null) {
            response.setShipments(
                    order.getShipments().stream()
                            .map(this::mapToShipmentResponse)
                            .collect(Collectors.toList())
            );
        }

        return response;
    }

    public OrderItemResponse mapToOrderItemResponse(OrderItem orderItem) {
        OrderItemResponse response = new OrderItemResponse();
        response.setId(orderItem.getId());
        response.setProductId(orderItem.getProduct().getId());
        response.setProductVariantId(orderItem.getVariant() != null ? orderItem.getVariant().getId() : null);
        response.setProductName(orderItem.getProductName());
        response.setVariantName(orderItem.getVariantName());
        response.setQuantity(orderItem.getQuantity());
        response.setUnitPrice(orderItem.getUnitPrice());
        response.setTotalPrice(orderItem.getTotalPrice());
        return response;
    }

    public PaymentResponse mapToPaymentResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setId(payment.getId());
        response.setPaymentReference(payment.getPaymentReference());
        response.setPaymentMethod(payment.getPaymentMethod());
        response.setStatus(payment.getStatus());
        response.setAmount(payment.getAmount());
        response.setGatewayTransactionId(payment.getGatewayTransactionId());
        response.setFailureReason(payment.getFailureReason());
        response.setCreatedAt(payment.getCreatedAt());
        response.setProcessedAt(payment.getProcessedAt());
        return response;
    }

    public ShipmentResponse mapToShipmentResponse(Shipment shipment) {
        ShipmentResponse response = new ShipmentResponse();
        response.setId(shipment.getId());
        response.setTrackingNumber(shipment.getTrackingNumber());
        response.setStatus(shipment.getStatus());
        response.setCarrier(shipment.getCarrier());
        response.setShippingMethod(shipment.getShippingMethod());
        response.setShippedDate(shipment.getShippedDate());
        response.setEstimatedDeliveryDate(shipment.getEstimatedDeliveryDate());
        response.setActualDeliveryDate(shipment.getActualDeliveryDate());
        response.setNotes(shipment.getNotes());
        response.setCreatedAt(shipment.getCreatedAt());

        if (shipment.getShipmentItems() != null) {
            response.setShipmentItems(
                    shipment.getShipmentItems().stream()
                            .map(this::mapToShipmentItemResponse)
                            .collect(Collectors.toList())
            );
        }

        return response;
    }

    public ShipmentItemResponse mapToShipmentItemResponse(ShipmentItem shipmentItem) {
        ShipmentItemResponse response = new ShipmentItemResponse();
        response.setId(shipmentItem.getId());
        response.setOrderItemId(shipmentItem.getOrderItem().getId());
        response.setQuantity(shipmentItem.getQuantity());
        response.setNotes(shipmentItem.getNotes());
        return response;
    }

    public InventoryResponse mapToInventoryResponse(Inventory inventory) {
        InventoryResponse response = new InventoryResponse();
        response.setId(inventory.getId());
        response.setWarehouseId(inventory.getWarehouse().getId());
        response.setWarehouseName(inventory.getWarehouse().getName());
        response.setProductId(inventory.getProduct().getId());
        response.setProductName(inventory.getProduct().getName());
        response.setProductVariantId(inventory.getProductVariant() != null ? inventory.getProductVariant().getId() : null);
        response.setProductVariantName(inventory.getProductVariant() != null ? inventory.getProductVariant().getName() : null);
        response.setQuantityOnHand(inventory.getQuantityOnHand());
        response.setQuantityReserved(inventory.getQuantityReserved());
        response.setQuantityAvailable(inventory.getQuantityAvailable());
        response.setReorderLevel(inventory.getReorderLevel());
        response.setMaxStockLevel(inventory.getMaxStockLevel());
        response.setStatus(inventory.getStatus());
        response.setLocation(inventory.getLocation());
        response.setNotes(inventory.getNotes());
        response.setCreatedAt(inventory.getCreatedAt());
        response.setUpdatedAt(inventory.getUpdatedAt());
        response.setLastStockUpdateAt(inventory.getLastStockUpdateAt());
        return response;
    }

    public ReviewResponse mapToReviewResponse(Review review, Long currentUserId) {
        ReviewResponse response = new ReviewResponse();
        response.setId(review.getId());
        response.setUserId(review.getUser().getId());

        // Handle anonymous reviews
        if (review.getIsAnonymous()) {
            response.setUserName("Anonymous");
        } else {
            response.setUserName(review.getUser().getFirstName() + " " + review.getUser().getLastName());
        }

        response.setProductId(review.getProduct().getId());
        response.setProductName(review.getProduct().getName());
        response.setOrderId(review.getOrder().getId());
        response.setRating(review.getRating());
        response.setTitle(review.getTitle());
        response.setReviewText(review.getReviewText());
        response.setStatus(review.getStatus());
        response.setIsVerifiedPurchase(review.getIsVerifiedPurchase());
        response.setIsAnonymous(review.getIsAnonymous());
        response.setHelpfulCount(review.getHelpfulCount());
        response.setUnhelpfulCount(review.getUnhelpfulCount());
        response.setSellerResponse(review.getSellerResponse());
        response.setSellerResponseAt(review.getSellerResponseAt());
        response.setCreatedAt(review.getCreatedAt());
        response.setUpdatedAt(review.getUpdatedAt());

        // Map review images
        if (review.getReviewImages() != null) {
            response.setImages(
                    review.getReviewImages().stream()
                            .map(this::mapToReviewImageResponse)
                            .sorted((a, b) -> a.getDisplayOrder().compareTo(b.getDisplayOrder()))
                            .collect(Collectors.toList())
            );
        }

        // Set current user's helpfulness vote
        if (currentUserId != null && review.getReviewHelpful() != null) {
            review.getReviewHelpful().stream()
                    .filter(rh -> rh.getUser().getId().equals(currentUserId))
                    .findFirst()
                    .ifPresent(rh -> response.setCurrentUserFoundHelpful(rh.getIsHelpful()));
        }

        return response;
    }

    public ReviewImageResponse mapToReviewImageResponse(ReviewImage reviewImage) {
        ReviewImageResponse response = new ReviewImageResponse();
        response.setId(reviewImage.getId());
        response.setImageUrl(reviewImage.getImageUrl());
        response.setAltText(reviewImage.getAltText());
        response.setDisplayOrder(reviewImage.getDisplayOrder());
        return response;
    }

    public ProductRatingResponse mapToProductRatingResponse(ProductRating productRating) {
        ProductRatingResponse response = new ProductRatingResponse();
        response.setProductId(productRating.getProduct().getId());
        response.setAverageRating(productRating.getAverageRating());
        response.setTotalReviews(productRating.getTotalReviews());
        response.setRating1Count(productRating.getRating1Count());
        response.setRating2Count(productRating.getRating2Count());
        response.setRating3Count(productRating.getRating3Count());
        response.setRating4Count(productRating.getRating4Count());
        response.setRating5Count(productRating.getRating5Count());
        response.setRating1Percentage(productRating.getRatingPercentage(1));
        response.setRating2Percentage(productRating.getRatingPercentage(2));
        response.setRating3Percentage(productRating.getRatingPercentage(3));
        response.setRating4Percentage(productRating.getRatingPercentage(4));
        response.setRating5Percentage(productRating.getRatingPercentage(5));
        response.setUpdatedAt(productRating.getUpdatedAt());
        return response;
    }
}