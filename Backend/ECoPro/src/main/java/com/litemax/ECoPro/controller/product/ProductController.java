package com.litemax.ECoPro.controller.product;

import com.litemax.ECoPro.dto.product.*;
import com.litemax.ECoPro.service.product.ProductService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Product Management", description = "Product management APIs")
public class ProductController {

    private final ProductService productService;

    // Public endpoints for browsing products

    @GetMapping
    @Operation(
        summary = "Get all products",
        description = "Retrieves paginated list of active products"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    })
    public ResponseEntity<Page<ProductListResponse>> getAllProducts(
            @Parameter(description = "Page number", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction", example = "desc")
            @RequestParam(defaultValue = "desc") String sortDirection) {

        log.debug("Getting all products - page: {}, size: {}, sortBy: {}, sortDirection: {}", 
                 page, size, sortBy, sortDirection);
        
        Page<ProductListResponse> products = productService.getAllActiveProducts(page, size, sortBy, sortDirection);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get product by ID",
        description = "Retrieves detailed product information by ID"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ProductResponse> getProductById(
            @Parameter(description = "Product ID", required = true)
            @PathVariable Long id) {
        
        log.info("Getting product by ID: {}", id);
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    @GetMapping("/slug/{slug}")
    @Operation(
            summary = "Get product by slug",
            description = "Retrieves detailed product information by slug"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ProductResponse> getProductBySlug(
            @Parameter(description = "Product slug", required = true)
            @PathVariable String slug) {

        log.info("Getting product by slug: {}", slug);
        ProductResponse product = productService.getProductBySlug(slug);
        return ResponseEntity.ok(product);
    }

    @PostMapping("/search")
    @Operation(
            summary = "Search products",
            description = "Advanced product search with filters and facets"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search completed successfully")
    })
    public ResponseEntity<ProductSearchResponse> searchProducts(
            @Parameter(description = "Search criteria", required = true)
            @Valid @RequestBody ProductSearchRequest searchRequest) {

        log.info("Searching products with criteria: {}", searchRequest);
        ProductSearchResponse searchResponse = productService.searchProducts(searchRequest);
        return ResponseEntity.ok(searchResponse);
    }

    @GetMapping("/featured")
    @Operation(
            summary = "Get featured products",
            description = "Retrieves list of featured products"
    )
    public ResponseEntity<List<ProductListResponse>> getFeaturedProducts(
            @Parameter(description = "Maximum number of products", example = "10")
            @RequestParam(defaultValue = "10") int limit) {

        log.debug("Getting featured products with limit: {}", limit);
        List<ProductListResponse> products = productService.getFeaturedProducts(limit);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/popular")
    @Operation(
            summary = "Get popular products",
            description = "Retrieves list of popular products based on views and ratings"
    )
    public ResponseEntity<Page<ProductListResponse>> getPopularProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Getting popular products - page: {}, size: {}", page, size);
        Page<ProductListResponse> products = productService.getPopularProducts(page, size);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/latest")
    @Operation(
            summary = "Get latest products",
            description = "Retrieves list of latest products"
    )
    public ResponseEntity<Page<ProductListResponse>> getLatestProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Getting latest products - page: {}, size: {}", page, size);
        Page<ProductListResponse> products = productService.getLatestProducts(page, size);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/category/{categoryId}")
    @Operation(
            summary = "Get products by category",
            description = "Retrieves products belonging to a specific category"
    )
    public ResponseEntity<Page<ProductListResponse>> getProductsByCategory(
            @Parameter(description = "Category ID", required = true)
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        log.debug("Getting products by category: {} - page: {}, size: {}", categoryId, page, size);
        Page<ProductListResponse> products = productService.getProductsByCategory(categoryId, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/brand/{brandId}")
    @Operation(
            summary = "Get products by brand",
            description = "Retrieves products belonging to a specific brand"
    )
    public ResponseEntity<Page<ProductListResponse>> getProductsByBrand(
            @Parameter(description = "Brand ID", required = true)
            @PathVariable Long brandId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        log.debug("Getting products by brand: {} - page: {}, size: {}", brandId, page, size);
        Page<ProductListResponse> products = productService.getProductsByBrand(brandId, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(products);
    }

    // Protected endpoints for product management

    @PostMapping
    @Operation(
            summary = "Create new product",
            description = "Creates a new product (Admin/Seller only)"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SELLER')")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid product data"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<ProductResponse> createProduct(
            @Parameter(description = "Product details", required = true)
            @Valid @RequestBody ProductRequest productRequest,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Creating new product: {} by user: {}", productRequest.getName(), userDetails.getUsername());
        ProductResponse product = productService.createProduct(productRequest, userDetails.getUsername());
        return new ResponseEntity<>(product, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update product",
            description = "Updates an existing product (Admin/Owner only)"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN') or @productService.isProductOwner(#id, authentication.name)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product updated successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<ProductResponse> updateProduct(
            @Parameter(description = "Product ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Updated product details", required = true)
            @Valid @RequestBody ProductRequest productRequest,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Updating product: {} by user: {}", id, userDetails.getUsername());
        ProductResponse product = productService.updateProduct(id, productRequest, userDetails.getUsername());
        return ResponseEntity.ok(product);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete product",
            description = "Soft deletes a product (Admin/Owner only)"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN') or @productService.isProductOwner(#id, authentication.name)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "Product ID", required = true)
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.warn("Deleting product: {} by user: {}", id, userDetails.getUsername());
        productService.deleteProduct(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @Operation(
            summary = "Update product status",
            description = "Updates product status (Admin/Owner only)"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN') or @productService.isProductOwner(#id, authentication.name)")
    public ResponseEntity<ProductResponse> updateProductStatus(
            @PathVariable Long id,
            @Parameter(description = "New product status", required = true)
            @RequestParam String status,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Updating product {} status to {} by user: {}", id, status, userDetails.getUsername());
        ProductResponse product = productService.updateProductStatus(id, status, userDetails.getUsername());
        return ResponseEntity.ok(product);
    }

    // Media management endpoints

    @PostMapping("/{id}/media")
    @Operation(
            summary = "Add product media",
            description = "Uploads and adds media to product"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN') or @productService.isProductOwner(#id, authentication.name)")
    public ResponseEntity<List<ProductMediaResponse>> addProductMedia(
            @PathVariable Long id,
            @Parameter(description = "Media files", required = true)
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(required = false) String altText,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Adding {} media files to product: {} by user: {}", files.size(), id, userDetails.getUsername());
        List<ProductMediaResponse> media = productService.addProductMedia(id, files, altText, userDetails.getUsername());
        return ResponseEntity.ok(media);
    }

    @DeleteMapping("/{id}/media/{mediaId}")
    @Operation(
            summary = "Remove product media",
            description = "Removes media from product"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN') or @productService.isProductOwner(#id, authentication.name)")
    public ResponseEntity<Void> removeProductMedia(
            @PathVariable Long id,
            @PathVariable Long mediaId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Removing media {} from product: {} by user: {}", mediaId, id, userDetails.getUsername());
        productService.removeProductMedia(id, mediaId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/media/{mediaId}/primary")
    @Operation(
            summary = "Set primary product media",
            description = "Sets media as primary for product"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN') or @productService.isProductOwner(#id, authentication.name)")
    public ResponseEntity<Void> setPrimaryMedia(
            @PathVariable Long id,
            @PathVariable Long mediaId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Setting media {} as primary for product: {} by user: {}", mediaId, id, userDetails.getUsername());
        productService.setPrimaryMedia(id, mediaId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    // Seller-specific endpoints

    @GetMapping("/seller/my-products")
    @Operation(
            summary = "Get seller's products",
            description = "Retrieves products belonging to the authenticated seller"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<Page<ProductListResponse>> getSellerProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("Getting seller products for user: {}", userDetails.getUsername());
        Page<ProductListResponse> products = productService.getSellerProducts(
                userDetails.getUsername(), page, size, sortBy, sortDirection, status);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/seller/dashboard")
    @Operation(
            summary = "Get seller dashboard data",
            description = "Retrieves dashboard statistics for seller"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<SellerDashboardResponse> getSellerDashboard(
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("Getting seller dashboard for user: {}", userDetails.getUsername());
        SellerDashboardResponse dashboard = productService.getSellerDashboard(userDetails.getUsername());
        return ResponseEntity.ok(dashboard);
    }

    // Product variants endpoints

    @PostMapping("/{id}/variants")
    @Operation(
            summary = "Add product variant",
            description = "Adds a new variant to the product"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN') or @productService.isProductOwner(#id, authentication.name)")
    public ResponseEntity<ProductVariantResponse> addProductVariant(
            @PathVariable Long id,
            @Valid @RequestBody ProductVariantRequest variantRequest,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Adding variant to product: {} by user: {}", id, userDetails.getUsername());
        ProductVariantResponse variant = productService.addProductVariant(id, variantRequest, userDetails.getUsername());
        return new ResponseEntity<>(variant, HttpStatus.CREATED);
    }

    @PutMapping("/{id}/variants/{variantId}")
    @Operation(
            summary = "Update product variant",
            description = "Updates an existing product variant"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN') or @productService.isProductOwner(#id, authentication.name)")
    public ResponseEntity<ProductVariantResponse> updateProductVariant(
            @PathVariable Long id,
            @PathVariable Long variantId,
            @Valid @RequestBody ProductVariantRequest variantRequest,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Updating variant {} for product: {} by user: {}", variantId, id, userDetails.getUsername());
        ProductVariantResponse variant = productService.updateProductVariant(id, variantId, variantRequest, userDetails.getUsername());
        return ResponseEntity.ok(variant);
    }

    @DeleteMapping("/{id}/variants/{variantId}")
    @Operation(
            summary = "Delete product variant",
            description = "Removes a product variant"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN') or @productService.isProductOwner(#id, authentication.name)")
    public ResponseEntity<Void> deleteProductVariant(
            @PathVariable Long id,
            @PathVariable Long variantId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.warn("Deleting variant {} from product: {} by user: {}", variantId, id, userDetails.getUsername());
        productService.deleteProductVariant(id, variantId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    // Inventory management

    @PatchMapping("/{id}/inventory")
    @Operation(
            summary = "Update product inventory",
            description = "Updates product inventory quantity"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN') or @productService.isProductOwner(#id, authentication.name)")
    public ResponseEntity<ProductResponse> updateInventory(
            @PathVariable Long id,
            @Parameter(description = "New inventory quantity", required = true)
            @RequestParam Integer quantity,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Updating inventory for product: {} to {} by user: {}", id, quantity, userDetails.getUsername());
        ProductResponse product = productService.updateInventory(id, quantity, userDetails.getUsername());
        return ResponseEntity.ok(product);
    }

    @PatchMapping("/{id}/variants/{variantId}/inventory")
    @Operation(
            summary = "Update variant inventory",
            description = "Updates product variant inventory quantity"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN') or @productService.isProductOwner(#id, authentication.name)")
    public ResponseEntity<ProductVariantResponse> updateVariantInventory(
            @PathVariable Long id,
            @PathVariable Long variantId,
            @RequestParam Integer quantity,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Updating inventory for variant {} in product: {} to {} by user: {}",
                variantId, id, quantity, userDetails.getUsername());
        ProductVariantResponse variant = productService.updateVariantInventory(id, variantId, quantity, userDetails.getUsername());
        return ResponseEntity.ok(variant);
    }
}