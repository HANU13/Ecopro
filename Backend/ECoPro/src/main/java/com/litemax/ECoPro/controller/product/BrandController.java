package com.litemax.ECoPro.controller.product;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.litemax.ECoPro.dto.product.BrandRequest;
import com.litemax.ECoPro.dto.product.BrandResponse;
import com.litemax.ECoPro.dto.product.BrandStatisticsResponse;
import com.litemax.ECoPro.service.product.BrandService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Brand Management", description = "Product brand management APIs")
public class BrandController {

    private final BrandService brandService;

    // Public endpoints

    @GetMapping
    @Operation(
            summary = "Get all brands",
            description = "Retrieves all active brands"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Brands retrieved successfully")
    })
    public ResponseEntity<Page<BrandResponse>> getAllBrands(
            @Parameter(description = "Page number", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Include inactive brands")
            @RequestParam(defaultValue = "false") boolean includeInactive) {

        log.debug("Getting all brands - page: {}, size: {}, includeInactive: {}", page, size, includeInactive);
        Page<BrandResponse> brands = brandService.getAllBrands(page, size, includeInactive);
        return ResponseEntity.ok(brands);
    }

    @GetMapping("/list")
    @Operation(
            summary = "Get brands list",
            description = "Retrieves all active brands as a simple list"
    )
    public ResponseEntity<List<BrandResponse>> getBrandsList() {
        log.debug("Getting brands list");
        List<BrandResponse> brands = brandService.getActiveBrands();
        return ResponseEntity.ok(brands);
    }

    @GetMapping("/featured")
    @Operation(
            summary = "Get featured brands",
            description = "Retrieves featured brands"
    )
    public ResponseEntity<List<BrandResponse>> getFeaturedBrands() {
        log.debug("Getting featured brands");
        List<BrandResponse> featuredBrands = brandService.getFeaturedBrands();
        return ResponseEntity.ok(featuredBrands);
    }

    @GetMapping("/popular")
    @Operation(
            summary = "Get popular brands",
            description = "Retrieves brands sorted by product count"
    )
    public ResponseEntity<Page<BrandResponse>> getPopularBrands(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Getting popular brands - page: {}, size: {}", page, size);
        Page<BrandResponse> popularBrands = brandService.getPopularBrands(page, size);
        return ResponseEntity.ok(popularBrands);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get brand by ID",
            description = "Retrieves brand details by ID"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Brand retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Brand not found")
    })
    public ResponseEntity<BrandResponse> getBrandById(
            @Parameter(description = "Brand ID", required = true)
            @PathVariable Long id) {

        log.info("Getting brand by ID: {}", id);
        BrandResponse brand = brandService.getBrandById(id);
        return ResponseEntity.ok(brand);
    }

    @GetMapping("/slug/{slug}")
    @Operation(
            summary = "Get brand by slug",
            description = "Retrieves brand details by slug"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Brand retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Brand not found")
    })
    public ResponseEntity<BrandResponse> getBrandBySlug(
            @Parameter(description = "Brand slug", required = true)
            @PathVariable String slug) {

        log.info("Getting brand by slug: {}", slug);
        BrandResponse brand = brandService.getBrandBySlug(slug);
        return ResponseEntity.ok(brand);
    }

    @GetMapping("/search")
    @Operation(
            summary = "Search brands",
            description = "Searches brands by keyword"
    )
    public ResponseEntity<Page<BrandResponse>> searchBrands(
            @Parameter(description = "Search keyword")
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Searching brands with keyword: {}", keyword);
        Page<BrandResponse> brands = brandService.searchBrands(keyword, page, size);
        return ResponseEntity.ok(brands);
    }

    // Protected endpoints for brand management

    @PostMapping
    @Operation(
            summary = "Create new brand",
            description = "Creates a new brand (Admin only)"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Brand created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid brand data"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<BrandResponse> createBrand(
            @Parameter(description = "Brand details", required = true)
            @Valid @RequestBody BrandRequest brandRequest,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Creating new brand: {} by user: {}", brandRequest.getName(), userDetails.getUsername());
        BrandResponse brand = brandService.createBrand(brandRequest);
        return new ResponseEntity<>(brand, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update brand",
            description = "Updates an existing brand (Admin only)"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Brand updated successfully"),
            @ApiResponse(responseCode = "404", description = "Brand not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<BrandResponse> updateBrand(
            @Parameter(description = "Brand ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Updated brand details", required = true)
            @Valid @RequestBody BrandRequest brandRequest,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Updating brand: {} by user: {}", id, userDetails.getUsername());
        BrandResponse brand = brandService.updateBrand(id, brandRequest);
        return ResponseEntity.ok(brand);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete brand",
            description = "Soft deletes a brand (Admin only)"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Brand deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Brand not found"),
            @ApiResponse(responseCode = "400", description = "Brand has associated products"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Void> deleteBrand(
            @Parameter(description = "Brand ID", required = true)
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.warn("Deleting brand: {} by user: {}", id, userDetails.getUsername());
        brandService.deleteBrand(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @Operation(
            summary = "Update brand status",
            description = "Activates or deactivates a brand (Admin only)"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BrandResponse> updateBrandStatus(
            @PathVariable Long id,
            @Parameter(description = "Active status", required = true)
            @RequestParam boolean active,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Updating brand {} status to {} by user: {}", id, active, userDetails.getUsername());
        BrandResponse brand = brandService.updateBrandStatus(id, active);
        return ResponseEntity.ok(brand);
    }

    @PatchMapping("/{id}/featured")
    @Operation(
            summary = "Update brand featured status",
            description = "Sets brand as featured or not (Admin only)"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BrandResponse> updateFeaturedStatus(
            @PathVariable Long id,
            @Parameter(description = "Featured status", required = true)
            @RequestParam boolean featured,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Updating brand {} featured status to {} by user: {}", id, featured, userDetails.getUsername());
        BrandResponse brand = brandService.updateFeaturedStatus(id, featured);
        return ResponseEntity.ok(brand);
    }

    // Admin statistics endpoints

    @GetMapping("/admin/statistics")
    @Operation(
            summary = "Get brand statistics",
            description = "Retrieves brand statistics (Admin only)"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BrandStatisticsResponse> getBrandStatistics() {
        log.debug("Getting brand statistics");
        BrandStatisticsResponse statistics = brandService.getBrandStatistics();
        return ResponseEntity.ok(statistics);
    }
}