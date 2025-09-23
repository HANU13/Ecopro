package com.litemax.ECoPro.service.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.litemax.ECoPro.dto.product.BrandResponse;
import com.litemax.ECoPro.dto.product.CategoryResponse;
import com.litemax.ECoPro.dto.product.ProductAttributeResponse;
import com.litemax.ECoPro.dto.product.ProductListResponse;
import com.litemax.ECoPro.dto.product.ProductMediaResponse;
import com.litemax.ECoPro.dto.product.ProductPriceRange;
import com.litemax.ECoPro.dto.product.ProductRequest;
import com.litemax.ECoPro.dto.product.ProductResponse;
import com.litemax.ECoPro.dto.product.ProductSearchRequest;
import com.litemax.ECoPro.dto.product.ProductSearchResponse;
import com.litemax.ECoPro.dto.product.ProductVariantRequest;
import com.litemax.ECoPro.dto.product.ProductVariantResponse;
import com.litemax.ECoPro.dto.product.SellerDashboardResponse;
import com.litemax.ECoPro.entity.auth.User;
import com.litemax.ECoPro.entity.product.Brand;
import com.litemax.ECoPro.entity.product.Category;
import com.litemax.ECoPro.entity.product.Product;
import com.litemax.ECoPro.entity.product.ProductAttribute;
import com.litemax.ECoPro.entity.product.ProductMedia;
import com.litemax.ECoPro.entity.product.ProductVariant;
import com.litemax.ECoPro.exception.ResourceNotFoundException;
import com.litemax.ECoPro.exception.ValidationException;
import com.litemax.ECoPro.repository.auth.UserRepository;
import com.litemax.ECoPro.repository.product.BrandRepository;
import com.litemax.ECoPro.repository.product.CategoryRepository;
import com.litemax.ECoPro.repository.product.ProductMediaRepository;
import com.litemax.ECoPro.repository.product.ProductRepository;
import com.litemax.ECoPro.repository.product.ProductVariantRepository;
import com.litemax.ECoPro.service.file.FileUploadService;
import com.litemax.ECoPro.util.SlugUtils;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductMediaRepository productMediaRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;
    private final FileUploadService fileUploadService;

    // Public browsing methods

    public Page<ProductListResponse> getAllActiveProducts(int page, int size, String sortBy, String sortDirection) {
        log.debug("Fetching active products - page: {}, size: {}, sortBy: {}, sortDirection: {}", 
                page, size, sortBy, sortDirection);

        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Product> products = productRepository.findByStatus(Product.ProductStatus.ACTIVE, pageable);
        return products.map(this::convertToListResponse);
    }

    public ProductResponse getProductById(Long id) {
        log.debug("Fetching product by ID: {}", id);
        
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
        
        // Increment view count
        productRepository.incrementViewCount(id);
        
        return convertToResponse(product);
    }

    public ProductResponse getProductBySlug(String slug) {
        log.debug("Fetching product by slug: {}", slug);
        
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with slug: " + slug));
        
        // Increment view count
        productRepository.incrementViewCount(product.getId());
        
        return convertToResponse(product);
    }

    public ProductSearchResponse searchProducts(ProductSearchRequest searchRequest) {
        log.debug("Searching products with request: {}", searchRequest);

        Specification<Product> spec = buildProductSpecification(searchRequest);
        
        Sort.Direction direction = searchRequest.getSortDirection().equalsIgnoreCase("desc") ? 
                Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(
                searchRequest.getPage(), 
                searchRequest.getSize(), 
                Sort.by(direction, searchRequest.getSortBy())
        );

        Page<Product> products = productRepository.findAll(spec, pageable);
        
        return ProductSearchResponse.builder()
                .products(products.getContent().stream()
                        .map(this::convertToListResponse)
                        .collect(Collectors.toList()))
                .totalElements(products.getTotalElements())
                .totalPages(products.getTotalPages())
                .currentPage(products.getNumber())
                .size(products.getSize())
                .hasNext(products.hasNext())
                .hasPrevious(products.hasPrevious())
                .categoryFacets(buildCategoryFacets(searchRequest))
                .brandFacets(buildBrandFacets(searchRequest))
                .priceRange(buildPriceRange())
                .build();
    }

    public List<ProductListResponse> getFeaturedProducts(int limit) {
        log.debug("Fetching featured products with limit: {}", limit);
        
        List<Product> products = productRepository.findByFeaturedTrueAndStatus(Product.ProductStatus.ACTIVE);
        return products.stream()
                .limit(limit)
                .map(this::convertToListResponse)
                .collect(Collectors.toList());
    }

    public Page<ProductListResponse> getPopularProducts(int page, int size) {
        log.debug("Fetching popular products - page: {}, size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productRepository.findPopularProducts(Product.ProductStatus.ACTIVE, pageable);
        return products.map(this::convertToListResponse);
    }

    public Page<ProductListResponse> getLatestProducts(int page, int size) {
        log.debug("Fetching latest products - page: {}, size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productRepository.findLatestProducts(Product.ProductStatus.ACTIVE, pageable);
        return products.map(this::convertToListResponse);
    }

    public Page<ProductListResponse> getProductsByCategory(Long categoryId, int page, int size, 
                                                          String sortBy, String sortDirection) {
        log.debug("Fetching products by category: {} - page: {}, size: {}", categoryId, page, size);
        
        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<Product> products = productRepository.findByCategoryIdAndStatus(
                categoryId, Product.ProductStatus.ACTIVE, pageable);
        return products.map(this::convertToListResponse);
    }

    public Page<ProductListResponse> getProductsByBrand(Long brandId, int page, int size, 
                                                       String sortBy, String sortDirection) {
        log.debug("Fetching products by brand: {} - page: {}, size: {}", brandId, page, size);
        
        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<Product> products = productRepository.findByBrandIdAndStatus(
                brandId, Product.ProductStatus.ACTIVE, pageable);
        return products.map(this::convertToListResponse);
    }

    // Product management methods

    public ProductResponse createProduct(ProductRequest request, String userEmail) {
        log.info("Creating new product: {} by user: {}", request.getName(), userEmail);

        // Validate unique constraints
        if (productRepository.existsBySku(request.getSku())) {
            throw new ValidationException("Product with SKU " + request.getSku() + " already exists");
        }

        User seller = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        Product product = new Product();
        updateProductFromRequest(product, request);
        product.setSeller(seller);
        product.setSlug(SlugUtils.createSlug(request.getName()));
        
        // Ensure slug uniqueness
        String originalSlug = product.getSlug();
        int counter = 1;
        while (productRepository.existsBySlug(product.getSlug())) {
            product.setSlug(originalSlug + "-" + counter++);
        }

        // Set published date if status is ACTIVE
        if (request.getStatus() == Product.ProductStatus.ACTIVE) {
            product.setPublishedAt(LocalDateTime.now());
        }

        product = productRepository.save(product);
        log.info("Product created with ID: {} by user: {}", product.getId(), userEmail);

        return convertToResponse(product);
    }

    public ProductResponse updateProduct(Long id, ProductRequest request, String userEmail) {
        log.info("Updating product: {} by user: {}", id, userEmail);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        // Check if SKU is being changed and if new SKU already exists
        if (!product.getSku().equals(request.getSku()) && 
            productRepository.existsBySku(request.getSku())) {
            throw new ValidationException("Product with SKU " + request.getSku() + " already exists");
        }

        // Update slug if name changed
        if (!product.getName().equals(request.getName())) {
            String newSlug = SlugUtils.createSlug(request.getName());
            if (!product.getSlug().equals(newSlug)) {
                // Ensure slug uniqueness
                String originalSlug = newSlug;
                int counter = 1;
                while (productRepository.existsBySlug(newSlug) && !newSlug.equals(product.getSlug())) {
                    newSlug = originalSlug + "-" + counter++;
                }
                product.setSlug(newSlug);
            }
        }

        updateProductFromRequest(product, request);

        // Set/update published date if status changed to ACTIVE
        if (request.getStatus() == Product.ProductStatus.ACTIVE && product.getPublishedAt() == null) {
            product.setPublishedAt(LocalDateTime.now());
        }

        product = productRepository.save(product);
        log.info("Product {} updated successfully by user: {}", id, userEmail);

        return convertToResponse(product);
    }

    public void deleteProduct(Long id, String userEmail) {
        log.warn("Deleting product: {} by user: {}", id, userEmail);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        // Soft delete by changing status to ARCHIVED
        product.setStatus(Product.ProductStatus.ARCHIVED);
        productRepository.save(product);

        log.warn("Product {} soft deleted by user: {}", id, userEmail);
    }

    public ProductResponse updateProductStatus(Long id, String status, String userEmail) {
        log.info("Updating product {} status to {} by user: {}", id, status, userEmail);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        Product.ProductStatus newStatus;
        try {
            newStatus = Product.ProductStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid product status: " + status);
        }

        product.setStatus(newStatus);

        // Set published date if changing to ACTIVE
        if (newStatus == Product.ProductStatus.ACTIVE && product.getPublishedAt() == null) {
            product.setPublishedAt(LocalDateTime.now());
        }

        product = productRepository.save(product);
        log.info("Product {} status updated to {} by user: {}", id, status, userEmail);

        return convertToResponse(product);
    }

    // Media management methods

    public List<ProductMediaResponse> addProductMedia(Long productId, List<MultipartFile> files, 
                                                     String altText, String userEmail) {
        log.info("Adding {} media files to product: {} by user: {}", files.size(), productId, userEmail);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

        List<ProductMediaResponse> mediaResponses = new ArrayList<>();
        
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            try {
                String fileUrl = fileUploadService.uploadFile(file, "products");
                
                ProductMedia media = new ProductMedia();
                media.setProduct(product);
                media.setFileUrl(fileUrl);
                media.setFileName(file.getOriginalFilename());
                media.setFileSize(file.getSize());
                media.setMimeType(file.getContentType());
                media.setAltText(altText);
                media.setSortOrder(product.getMedia().size() + i);
                
                // Set first image as primary if no primary exists
                if (product.getMedia().isEmpty() && i == 0) {
                    media.setPrimary(true);
                }
                
                media = productMediaRepository.save(media);
                mediaResponses.add(convertToMediaResponse(media));
                
            } catch (Exception e) {
                log.error("Failed to upload media file: {}", file.getOriginalFilename(), e);
                throw new ValidationException("Failed to upload media file: " + file.getOriginalFilename());
            }
        }

        log.info("Added {} media files to product: {}", mediaResponses.size(), productId);
        return mediaResponses;
    }

    public void removeProductMedia(Long productId, Long mediaId, String userEmail) {
        log.info("Removing media {} from product: {} by user: {}", mediaId, productId, userEmail);

        ProductMedia media = productMediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with ID: " + mediaId));

        if (!media.getProduct().getId().equals(productId)) {
            throw new ValidationException("Media does not belong to this product");
        }

        // If removing primary image, set another image as primary
        if (media.isPrimary()) {
            List<ProductMedia> otherMedia = productMediaRepository.findByProductIdOrderBySortOrder(productId);
            otherMedia.stream()
                    .filter(m -> !m.getId().equals(mediaId))
                    .findFirst()
                    .ifPresent(m -> {
                        m.setPrimary(true);
                        productMediaRepository.save(m);
                    });
        }

        productMediaRepository.delete(media);
        log.info("Media {} removed from product: {}", mediaId, productId);
    }

    public void setPrimaryMedia(Long productId, Long mediaId, String userEmail) {
        log.info("Setting media {} as primary for product: {} by user: {}", mediaId, productId, userEmail);

        ProductMedia media = productMediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with ID: " + mediaId));

        if (!media.getProduct().getId().equals(productId)) {
            throw new ValidationException("Media does not belong to this product");
        }

        // Reset all media for this product to non-primary
        productMediaRepository.resetPrimaryForProduct(productId);

        // Set this media as primary
        media.setPrimary(true);
        productMediaRepository.save(media);

        log.info("Media {} set as primary for product: {}", mediaId, productId);
    }

    // Seller-specific methods

    public Page<ProductListResponse> getSellerProducts(String userEmail, int page, int size, 
                                                      String sortBy, String sortDirection, String status) {
        log.debug("Fetching seller products for user: {}", userEmail);

        User seller = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Product> products;
        if (status != null) {
            Product.ProductStatus productStatus = Product.ProductStatus.valueOf(status.toUpperCase());
            products = productRepository.findBySellerIdAndStatus(seller.getId(), productStatus, pageable);
        } else {
            products = productRepository.findBySellerId(seller.getId(), pageable);
        }

        return products.map(this::convertToListResponse);
    }

    public SellerDashboardResponse getSellerDashboard(String userEmail) {
        log.debug("Getting seller dashboard for user: {}", userEmail);

        User seller = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        Long sellerId = seller.getId();
        
        long totalProducts = productRepository.countBySellerIdAndStatus(sellerId, null);
        long activeProducts = productRepository.countBySellerIdAndStatus(sellerId, Product.ProductStatus.ACTIVE);
        long draftProducts = productRepository.countBySellerIdAndStatus(sellerId, Product.ProductStatus.DRAFT);

        // Get low stock and out of stock products
        List<Product> sellerProducts = productRepository.findBySellerIdAndStatus(sellerId, Product.ProductStatus.ACTIVE);
        long outOfStockProducts = sellerProducts.stream()
                .filter(p -> !p.isInStock())
                .count();
        long lowStockProducts = sellerProducts.stream()
                .filter(Product::isLowStock)
                .count();

        // Get top products (by views)
        List<ProductListResponse> topProducts = sellerProducts.stream()
                .sorted((p1, p2) -> p2.getViewCount().compareTo(p1.getViewCount()))
                .limit(5)
                .map(this::convertToListResponse)
                .collect(Collectors.toList());

        return SellerDashboardResponse.builder()
                .totalProducts(totalProducts)
                .activeProducts(activeProducts)
                .draftProducts(draftProducts)
                .outOfStockProducts(outOfStockProducts)
                .lowStockProducts(lowStockProducts)
                .totalRevenue(BigDecimal.ZERO) // Would be calculated from orders
                .totalViews(sellerProducts.stream().mapToLong(Product::getViewCount).sum())
                .averageRating(BigDecimal.ZERO) // Would be calculated from reviews
                .topProducts(topProducts)
                .recentActivity(SellerDashboardResponse.RecentActivitySummary.builder()
                        .productsAddedThisWeek(0L)
                        .ordersThisWeek(0L)
                        .viewsThisWeek(0L)
                        .build())
                .build();
    }

    // Variant management methods

    public ProductVariantResponse addProductVariant(Long productId, ProductVariantRequest request, String userEmail) {
        log.info("Adding variant to product: {} by user: {}", productId, userEmail);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

        // Validate unique SKU
        if (productVariantRepository.existsBySku(request.getSku())) {
            throw new ValidationException("Variant with SKU " + request.getSku() + " already exists");
        }

        ProductVariant variant = new ProductVariant();
        updateVariantFromRequest(variant, request);
        variant.setProduct(product);

        variant = productVariantRepository.save(variant);
        log.info("Variant added to product: {} with ID: {}", productId, variant.getId());

        return convertToVariantResponse(variant);
    }

    public ProductVariantResponse updateProductVariant(Long productId, Long variantId,
                                                       ProductVariantRequest request, String userEmail) {
        log.info("Updating variant {} for product: {} by user: {}", variantId, productId, userEmail);

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with ID: " + variantId));

        if (!variant.getProduct().getId().equals(productId)) {
            throw new ValidationException("Variant does not belong to this product");
        }

        // Check if SKU is being changed and if new SKU already exists
        if (!variant.getSku().equals(request.getSku()) &&
                productVariantRepository.existsBySku(request.getSku())) {
            throw new ValidationException("Variant with SKU " + request.getSku() + " already exists");
        }

        updateVariantFromRequest(variant, request);
        variant = productVariantRepository.save(variant);

        log.info("Variant {} updated successfully", variantId);
        return convertToVariantResponse(variant);
    }

    public void deleteProductVariant(Long productId, Long variantId, String userEmail) {
        log.warn("Deleting variant {} from product: {} by user: {}", variantId, productId, userEmail);

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with ID: " + variantId));

        if (!variant.getProduct().getId().equals(productId)) {
            throw new ValidationException("Variant does not belong to this product");
        }

        productVariantRepository.delete(variant);
        log.warn("Variant {} deleted from product: {}", variantId, productId);
    }

    // Inventory management methods

    public ProductResponse updateInventory(Long productId, Integer quantity, String userEmail) {
        log.info("Updating inventory for product: {} to {} by user: {}", productId, quantity, userEmail);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

        product.setInventoryQuantity(quantity);
        updateInventoryStatus(product);

        product = productRepository.save(product);
        log.info("Inventory updated for product: {}", productId);

        return convertToResponse(product);
    }

    public ProductVariantResponse updateVariantInventory(Long productId, Long variantId,
                                                         Integer quantity, String userEmail) {
        log.info("Updating inventory for variant {} in product: {} to {} by user: {}",
                variantId, productId, quantity, userEmail);

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with ID: " + variantId));

        if (!variant.getProduct().getId().equals(productId)) {
            throw new ValidationException("Variant does not belong to this product");
        }

        variant.setInventoryQuantory(quantity);
        variant = productVariantRepository.save(variant);

        log.info("Inventory updated for variant: {}", variantId);
        return convertToVariantResponse(variant);
    }

    // Utility methods

    public boolean isProductOwner(Long productId, String userEmail) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null || product.getSeller() == null) {
            return false;
        }
        return product.getSeller().getEmail().equals(userEmail);
    }

    // Private helper methods

    private void updateProductFromRequest(Product product, ProductRequest request) {
        product.setName(request.getName());
        product.setShortDescription(request.getShortDescription());
        product.setDescription(request.getDescription());
        product.setSku(request.getSku());
        product.setPrice(request.getPrice());
        product.setComparePrice(request.getComparePrice());
        product.setCostPrice(request.getCostPrice());
        product.setTrackInventory(request.isTrackInventory());
        product.setInventoryQuantity(request.getInventoryQuantity());
        product.setLowStockThreshold(request.getLowStockThreshold());
        product.setAllowBackorders(request.isAllowBackorders());
        product.setStatus(request.getStatus());
        product.setFeatured(request.isFeatured());
        product.setDigital(request.isDigital());
        product.setRequiresShipping(request.isRequiresShipping());
        product.setWeight(request.getWeight());
        product.setDimensions(request.getDimensions());
        product.setMetaTitle(request.getMetaTitle());
        product.setMetaDescription(request.getMetaDescription());
        product.setMetaKeywords(request.getMetaKeywords());
        product.setTags(request.getTags());
        product.setVendor(request.getVendor());
        product.setBarcode(request.getBarcode());
        product.setHsnCode(request.getHsnCode());
        product.setTaxPercentage(request.getTaxPercentage());
        product.setSortOrder(request.getSortOrder());

        // Set brand
        if (request.getBrandId() != null) {
            Brand brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new ResourceNotFoundException("Brand not found with ID: " + request.getBrandId()));
            product.setBrand(brand);
        }

        // Set categories
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            Set<Category> categories = new HashSet<>();
            for (Long categoryId : request.getCategoryIds()) {
                Category category = categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));
                categories.add(category);
            }
            product.setCategories(categories);
        }

        // Update inventory status
        updateInventoryStatus(product);
    }

    private void updateVariantFromRequest(ProductVariant variant, ProductVariantRequest request) {
        variant.setName(request.getName());
        variant.setSku(request.getSku());
        variant.setOption1Name(request.getOption1Name());
        variant.setOption1Value(request.getOption1Value());
        variant.setOption2Name(request.getOption2Name());
        variant.setOption2Value(request.getOption2Value());
        variant.setOption3Name(request.getOption3Name());
        variant.setOption3Value(request.getOption3Value());
        variant.setPrice(request.getPrice());
        variant.setComparePrice(request.getComparePrice());
        variant.setCostPrice(request.getCostPrice());
        variant.setInventoryQuantory(request.getInventoryQuantory());
        variant.setInventoryPolicy(request.getInventoryPolicy());
        variant.setWeight(request.getWeight());
        variant.setBarcode(request.getBarcode());
        variant.setImageUrl(request.getImageUrl());
        variant.setActive(request.isActive());
        variant.setSortOrder(request.getSortOrder());
    }

    private void updateInventoryStatus(Product product) {
        if (!product.isLowStock()) {
            product.setInventoryStatus(Product.InventoryStatus.IN_STOCK);
            return;
        }

        Integer quantity = product.getInventoryQuantity();
        if (quantity == null || quantity <= 0) {
            product.setInventoryStatus(Product.InventoryStatus.OUT_OF_STOCK);
        } else if (quantity <= product.getLowStockThreshold()) {
            product.setInventoryStatus(Product.InventoryStatus.LOW_STOCK);
        } else {
            product.setInventoryStatus(Product.InventoryStatus.IN_STOCK);
        }
    }

    private Specification<Product> buildProductSpecification(ProductSearchRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Default to active products
            predicates.add(criteriaBuilder.equal(root.get("status"), Product.ProductStatus.valueOf(request.getStatus())));

            // Keyword search
            if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
                String keyword = "%" + request.getKeyword().toLowerCase() + "%";
                Predicate keywordPredicate = criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), keyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), keyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("tags")), keyword)
                );
                predicates.add(keywordPredicate);
            }

            // Category filter
            if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
                predicates.add(root.join("categories").get("id").in(request.getCategoryIds()));
            }

            // Brand filter
            if (request.getBrandIds() != null && !request.getBrandIds().isEmpty()) {
                predicates.add(root.get("brand").get("id").in(request.getBrandIds()));
            }

            // Price range filter
            if (request.getMinPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), request.getMinPrice()));
            }
            if (request.getMaxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), request.getMaxPrice()));
            }

            // Rating filter
            if (request.getMinRating() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("rating"), request.getMinRating()));
            }

            // Featured filter
            if (request.getFeatured() != null) {
                predicates.add(criteriaBuilder.equal(root.get("featured"), request.getFeatured()));
            }

            // In stock filter
            if (request.getInStock() != null && request.getInStock()) {
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.equal(root.get("trackInventory"), false),
                        criteriaBuilder.greaterThan(root.get("inventoryQuantity"), 0)
                ));
            }

            // Vendor filter
            if (request.getVendor() != null && !request.getVendor().trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("vendor")),
                        request.getVendor().toLowerCase()
                ));
            }

            // Digital filter
            if (request.getDigital() != null) {
                predicates.add(criteriaBuilder.equal(root.get("digital"), request.getDigital()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Map<String, Long> buildCategoryFacets(ProductSearchRequest request) {
        // This would typically be implemented with a separate query to get category counts
        // For now, returning empty map
        return new HashMap<>();
    }

    private Map<String, Long> buildBrandFacets(ProductSearchRequest request) {
        // This would typically be implemented with a separate query to get brand counts
        // For now, returning empty map
        return new HashMap<>();
    }

    private ProductPriceRange buildPriceRange() {
        // This would typically query for min and max prices in the database
        return ProductPriceRange.builder()
                .minPrice(BigDecimal.ZERO)
                .maxPrice(new BigDecimal("10000"))
                .build();
    }

    // Conversion methods

    private ProductResponse convertToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .shortDescription(product.getShortDescription())
                .description(product.getDescription())
                .sku(product.getSku())
                .price(product.getPrice())
                .comparePrice(product.getComparePrice())
                .costPrice(product.getCostPrice())
                .trackInventory(product.isTrackInventory())
                .inventoryQuantity(product.getInventoryQuantity())
                .lowStockThreshold(product.getLowStockThreshold())
                .allowBackorders(product.isAllowBackorders())
                .status(product.getStatus())
                .inventoryStatus(product.getInventoryStatus())
                .featured(product.isFeatured())
                .digital(product.isDigital())
                .requiresShipping(product.isRequiresShipping())
                .weight(product.getWeight())
                .dimensions(product.getDimensions())
                .metaTitle(product.getMetaTitle())
                .metaDescription(product.getMetaDescription())
                .metaKeywords(product.getMetaKeywords())
                .tags(product.getTags())
                .vendor(product.getVendor())
                .barcode(product.getBarcode())
                .hsnCode(product.getHsnCode())
                .taxPercentage(product.getTaxPercentage())
                .sortOrder(product.getSortOrder())
                .viewCount(product.getViewCount())
                .rating(product.getRating())
                .reviewCount(product.getReviewCount())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .publishedAt(product.getPublishedAt())
                .seller(convertToUserProfile(product.getSeller()))
                .brand(product.getBrand() != null ? convertToBrandResponse(product.getBrand()) : null)
                .categories(product.getCategories().stream()
                        .map(this::convertToCategoryResponse)
                        .collect(Collectors.toList()))
                .media(product.getMedia().stream()
                        .sorted(Comparator.comparing(ProductMedia::getSortOrder))
                        .map(this::convertToMediaResponse)
                        .collect(Collectors.toList()))
                .variants(product.getVariants().stream()
                        .sorted(Comparator.comparing(ProductVariant::getSortOrder))
                        .map(this::convertToVariantResponse)
                        .collect(Collectors.toList()))
                .attributes(product.getAttributes().stream()
                        .sorted(Comparator.comparing(ProductAttribute::getSortOrder))
                        .map(this::convertToAttributeResponse)
                        .collect(Collectors.toList()))
                .inStock(product.isInStock())
                .lowStock(product.isLowStock())
                .discountPercentage(product.getDiscountPercentage())
                .primaryImageUrl(product.getPrimaryImage() != null ? product.getPrimaryImage().getFileUrl() : null)
                .tagsList(product.getTagsList())
                .formattedPrice(formatPrice(product.getPrice()))
                .formattedComparePrice(product.getComparePrice() != null ? formatPrice(product.getComparePrice()) : null)
                .hasDiscount(product.getDiscountPercentage().compareTo(BigDecimal.ZERO) > 0)
                .stockStatus(getStockStatus(product))
                .build();
    }

    private ProductListResponse convertToListResponse(Product product) {
        return ProductListResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .shortDescription(product.getShortDescription())
                .sku(product.getSku())
                .price(product.getPrice())
                .comparePrice(product.getComparePrice())
                .status(product.getStatus())
                .featured(product.isFeatured())
                .viewCount(product.getViewCount())
                .rating(product.getRating())
                .reviewCount(product.getReviewCount())
                .createdAt(product.getCreatedAt())
                .sellerName(product.getSeller() != null ? product.getSeller().getFullName() : null)
                .brandName(product.getBrand() != null ? product.getBrand().getName() : null)
                .categoryNames(product.getCategories().stream()
                        .map(Category::getName)
                        .collect(Collectors.toList()))
                .primaryImageUrl(product.getPrimaryImage() != null ? product.getPrimaryImage().getFileUrl() : null)
                .inStock(product.isInStock())
                .discountPercentage(product.getDiscountPercentage())
                .hasDiscount(product.getDiscountPercentage().compareTo(BigDecimal.ZERO) > 0)
                .stockStatus(getStockStatus(product))
                .formattedPrice(formatPrice(product.getPrice()))
                .formattedComparePrice(product.getComparePrice() != null ? formatPrice(product.getComparePrice()) : null)
                .build();
    }

    private ProductMediaResponse convertToMediaResponse(ProductMedia media) {
        return ProductMediaResponse.builder()
                .id(media.getId())
                .fileUrl(media.getFileUrl())
                .fileName(media.getFileName())
                .fileSize(media.getFileSize())
                .mimeType(media.getMimeType())
                .type(media.getType())
                .altText(media.getAltText())
                .primary(media.isPrimary())
                .sortOrder(media.getSortOrder())
                .width(media.getWidth())
                .height(media.getHeight())
                .createdAt(media.getCreatedAt())
                .build();
    }

    private ProductVariantResponse convertToVariantResponse(ProductVariant variant) {
        return ProductVariantResponse.builder()
                .id(variant.getId())
                .name(variant.getName())
                .sku(variant.getSku())
                .option1Name(variant.getOption1Name())
                .option1Value(variant.getOption1Value())
                .option2Name(variant.getOption2Name())
                .option2Value(variant.getOption2Value())
                .option3Name(variant.getOption3Name())
                .option3Value(variant.getOption3Value())
                .price(variant.getPrice())
                .comparePrice(variant.getComparePrice())
                .costPrice(variant.getCostPrice())
                .inventoryQuantory(variant.getInventoryQuantory())
                .inventoryPolicy(variant.getInventoryPolicy())
                .weight(variant.getWeight())
                .barcode(variant.getBarcode())
                .imageUrl(variant.getImageUrl())
                .active(variant.isActive())
                .sortOrder(variant.getSortOrder())
                .createdAt(variant.getCreatedAt())
                .updatedAt(variant.getUpdatedAt())
                .displayName(variant.getDisplayName())
                .inStock(variant.isInStock())
                .discountPercentage(variant.getDiscountPercentage())
                .formattedPrice(formatPrice(variant.getPrice()))
                .formattedComparePrice(variant.getComparePrice() != null ? formatPrice(variant.getComparePrice()) : null)
                .build();
    }

    private ProductAttributeResponse convertToAttributeResponse(ProductAttribute attribute) {
        return ProductAttributeResponse.builder()
                .id(attribute.getId())
                .attributeId(attribute.getAttribute().getId())
                .attributeName(attribute.getAttribute().getName())
                .attributeCode(attribute.getAttribute().getCode())
                .value(attribute.getValue())
                .sortOrder(attribute.getSortOrder())
                .build();
    }

    private CategoryResponse convertToCategoryResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .iconUrl(category.getIconUrl())
                .active(category.isActive())
                .featured(category.isFeatured())
                .sortOrder(category.getSortOrder())
                .productCount(category.getProductCount())
                .hasChildren(category.hasChildren())
                .fullPath(category.getFullPath())
                .build();
    }

    private BrandResponse convertToBrandResponse(Brand brand) {
        return BrandResponse.builder()
                .id(brand.getId())
                .name(brand.getName())
                .slug(brand.getSlug())
                .description(brand.getDescription())
                .logoUrl(brand.getLogoUrl())
                .bannerUrl(brand.getBannerUrl())
                .websiteUrl(brand.getWebsiteUrl())
                .active(brand.isActive())
                .featured(brand.isFeatured())
                .sortOrder(brand.getSortOrder())
                .productCount(brand.getProductCount())
                .originCountry(brand.getOriginCountry())
                .build();
    }

    private com.litemax.ECoPro.dto.auth.UserResponse convertToUserProfile(User user) {
        if (user == null) return null;

        return com.litemax.ECoPro.dto.auth.UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) return null;
        return String.format("$%.2f", price);
    }

    private String getStockStatus(Product product) {
        if (!product.isLowStock()) return "IN_STOCK";
        if (product.getInventoryQuantity() == null || product.getInventoryQuantity() <= 0) return "OUT_OF_STOCK";
        if (product.getInventoryQuantity() <= product.getLowStockThreshold()) return "LOW_STOCK";
        return "IN_STOCK";
    }
}