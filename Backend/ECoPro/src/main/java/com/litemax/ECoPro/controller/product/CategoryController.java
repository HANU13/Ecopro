package com.litemax.ECoPro.controller.product;

import com.litemax.ECoPro.dto.product.CategoryRequest;
import com.litemax.ECoPro.dto.product.CategoryResponse;
import com.litemax.ECoPro.dto.product.CategoryStatisticsResponse;
import com.litemax.ECoPro.service.product.CategoryService;
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

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Category Management", description = "Product category management APIs")
public class CategoryController {

    private final CategoryService categoryService;

    // Public endpoints

    @GetMapping
    @Operation(
        summary = "Get all categories",
        description = "Retrieves all active categories with hierarchy"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Categories retrieved successfully")
    })
    public ResponseEntity<List<CategoryResponse>> getAllCategories(
            @Parameter(description = "Include inactive categories")
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        
        log.debug("Getting all categories - includeInactive: {}", includeInactive);
        List<CategoryResponse> categories = categoryService.getAllCategories(includeInactive);
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/tree")
    @Operation(
        summary = "Get category tree",
        description = "Retrieves categories in hierarchical tree structure"
    )
    public ResponseEntity<List<CategoryResponse>> getCategoryTree() {
        log.debug("Getting category tree");
        List<CategoryResponse> categoryTree = categoryService.getCategoryTree();
        return ResponseEntity.ok(categoryTree);
    }

    @GetMapping("/root")
    @Operation(
        summary = "Get root categories",
        description = "Retrieves top-level categories"
    )
    public ResponseEntity<List<CategoryResponse>> getRootCategories() {
        log.debug("Getting root categories");
        List<CategoryResponse> rootCategories = categoryService.getRootCategories();
        return ResponseEntity.ok(rootCategories);
    }

    @GetMapping("/featured")
    @Operation(
        summary = "Get featured categories",
        description = "Retrieves featured categories"
    )
    public ResponseEntity<List<CategoryResponse>> getFeaturedCategories() {
        log.debug("Getting featured categories");
        List<CategoryResponse> featuredCategories = categoryService.getFeaturedCategories();
        return ResponseEntity.ok(featuredCategories);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get category by ID",
        description = "Retrieves category details by ID"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Category retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Category not found")
    })
    public ResponseEntity<CategoryResponse> getCategoryById(
            @Parameter(description = "Category ID", required = true)
            @PathVariable Long id) {
        
        log.info("Getting category by ID: {}", id);
        CategoryResponse category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(category);
    }

    @GetMapping("/slug/{slug}")
    @Operation(
        summary = "Get category by slug",
        description = "Retrieves category details by slug"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Category retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Category not found")
    })
    public ResponseEntity<CategoryResponse> getCategoryBySlug(
            @Parameter(description = "Category slug", required = true)
            @PathVariable String slug) {
        
        log.info("Getting category by slug: {}", slug);
        CategoryResponse category = categoryService.getCategoryBySlug(slug);
        return ResponseEntity.ok(category);
    }

    @GetMapping("/{id}/children")
    @Operation(
        summary = "Get category children",
        description = "Retrieves child categories of a category"
    )
    public ResponseEntity<List<CategoryResponse>> getCategoryChildren(
            @Parameter(description = "Parent category ID", required = true)
            @PathVariable Long id) {
        
        log.debug("Getting children for category: {}", id);
        List<CategoryResponse> children = categoryService.getCategoryChildren(id);
        return ResponseEntity.ok(children);
    }

    @GetMapping("/search")
    @Operation(
        summary = "Search categories",
        description = "Searches categories by keyword"
    )
    public ResponseEntity<Page<CategoryResponse>> searchCategories(
            @Parameter(description = "Search keyword")
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("Searching categories with keyword: {}", keyword);
        Page<CategoryResponse> categories = categoryService.searchCategories(keyword, page, size);
        return ResponseEntity.ok(categories);
    }

    // Protected endpoints for category management

    @PostMapping
    @Operation(
        summary = "Create new category",
        description = "Creates a new category (Admin only)"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Category created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid category data"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<CategoryResponse> createCategory(
            @Parameter(description = "Category details", required = true)
            @Valid @RequestBody CategoryRequest categoryRequest,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Creating new category: {} by user: {}", categoryRequest.getName(), userDetails.getUsername());
        CategoryResponse category = categoryService.createCategory(categoryRequest);
        return new ResponseEntity<>(category, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Update category",
        description = "Updates an existing category (Admin only)"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Category updated successfully"),
        @ApiResponse(responseCode = "404", description = "Category not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<CategoryResponse> updateCategory(
            @Parameter(description = "Category ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Updated category details", required = true)
            @Valid @RequestBody CategoryRequest categoryRequest,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Updating category: {} by user: {}", id, userDetails.getUsername());
        CategoryResponse category = categoryService.updateCategory(id, categoryRequest);
        return ResponseEntity.ok(category);
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete category",
        description = "Soft deletes a category (Admin only)"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Category deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Category not found"),
        @ApiResponse(responseCode = "400", description = "Category has child categories or products"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Void> deleteCategory(
            @Parameter(description = "Category ID", required = true)
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.warn("Deleting category: {} by user: {}", id, userDetails.getUsername());
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @Operation(
        summary = "Update category status",
        description = "Activates or deactivates a category (Admin only)"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> updateCategoryStatus(
            @PathVariable Long id,
            @Parameter(description = "Active status", required = true)
            @RequestParam boolean active,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Updating category {} status to {} by user: {}", id, active, userDetails.getUsername());
        CategoryResponse category = categoryService.updateCategoryStatus(id, active);
        return ResponseEntity.ok(category);
    }

    @PatchMapping("/{id}/featured")
    @Operation(
        summary = "Update category featured status",
        description = "Sets category as featured or not (Admin only)"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> updateFeaturedStatus(
            @PathVariable Long id,
            @Parameter(description = "Featured status", required = true)
            @RequestParam boolean featured,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Updating category {} featured status to {} by user: {}", id, featured, userDetails.getUsername());
        CategoryResponse category = categoryService.updateFeaturedStatus(id, featured);
        return ResponseEntity.ok(category);
    }

    @PutMapping("/{id}/sort-order")
    @Operation(
        summary = "Update category sort order",
        description = "Updates category sort order (Admin only)"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> updateSortOrder(
            @PathVariable Long id,
            @Parameter(description = "Sort order", required = true)
            @RequestParam Integer sortOrder,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Updating category {} sort order to {} by user: {}", id, sortOrder, userDetails.getUsername());
        CategoryResponse category = categoryService.updateSortOrder(id, sortOrder);
        return ResponseEntity.ok(category);
    }

    @PutMapping("/{id}/parent")
    @Operation(
        summary = "Update category parent",
        description = "Changes category parent (Admin only)"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> updateParent(
            @PathVariable Long id,
            @Parameter(description = "New parent category ID (null for root category)")
            @RequestParam(required = false) Long parentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Updating category {} parent to {} by user: {}", id, parentId, userDetails.getUsername());
        CategoryResponse category = categoryService.updateParent(id, parentId);
        return ResponseEntity.ok(category);
    }

    // Admin statistics endpoints

    @GetMapping("/admin/statistics")
    @Operation(
        summary = "Get category statistics",
        description = "Retrieves category statistics (Admin only)"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryStatisticsResponse> getCategoryStatistics() {
        log.debug("Getting category statistics");
        CategoryStatisticsResponse statistics = categoryService.getCategoryStatistics();
        return ResponseEntity.ok(statistics);
    }
}