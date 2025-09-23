package com.litemax.ECoPro.service.product;

import com.litemax.ECoPro.dto.product.CategoryRequest;
import com.litemax.ECoPro.dto.product.CategoryResponse;
import com.litemax.ECoPro.dto.product.CategoryStatisticsResponse;
import com.litemax.ECoPro.entity.product.Category;
import com.litemax.ECoPro.entity.product.Product;
import com.litemax.ECoPro.exception.ResourceNotFoundException;
import com.litemax.ECoPro.exception.ValidationException;
import com.litemax.ECoPro.repository.product.CategoryRepository;
import com.litemax.ECoPro.repository.product.ProductRepository;
import com.litemax.ECoPro.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    // Public browsing methods

    @Cacheable(value = "categories", key = "'all_categories_' + #includeInactive")
    public List<CategoryResponse> getAllCategories(boolean includeInactive) {
        log.debug("Fetching all categories - includeInactive: {}", includeInactive);

        List<Category> categories;
        if (includeInactive) {
            categories = categoryRepository.findAllOrderedByHierarchy();
        } else {
            categories = categoryRepository.findByActiveTrueOrderBySortOrder();
        }

        log.debug("Found {} categories", categories.size());
        return categories.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "categories", key = "'category_tree'")
    public List<CategoryResponse> getCategoryTree() {
        log.debug("Fetching category tree structure");

        List<Category> rootCategories = categoryRepository.findRootCategoriesOrdered();
        
        List<CategoryResponse> categoryTree = rootCategories.stream()
                .map(this::convertToResponseWithChildren)
                .collect(Collectors.toList());

        log.debug("Generated category tree with {} root categories", categoryTree.size());
        return categoryTree;
    }

    @Cacheable(value = "categories", key = "'root_categories'")
    public List<CategoryResponse> getRootCategories() {
        log.debug("Fetching root categories");

        List<Category> rootCategories = categoryRepository.findByParentIsNullAndActiveTrue();
        
        log.debug("Found {} root categories", rootCategories.size());
        return rootCategories.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "categories", key = "'featured_categories'")
    public List<CategoryResponse> getFeaturedCategories() {
        log.debug("Fetching featured categories");

        List<Category> featuredCategories = categoryRepository.findByFeaturedTrueAndActiveTrueOrderBySortOrder();
        
        log.debug("Found {} featured categories", featuredCategories.size());
        return featuredCategories.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "category", key = "#id")
    public CategoryResponse getCategoryById(Long id) {
        log.debug("Fetching category by ID: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

        log.debug("Found category: {} (ID: {})", category.getName(), id);
        return convertToResponseWithChildren(category);
    }

    @Cacheable(value = "category", key = "'slug_' + #slug")
    public CategoryResponse getCategoryBySlug(String slug) {
        log.debug("Fetching category by slug: {}", slug);

        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with slug: " + slug));

        log.debug("Found category: {} (slug: {})", category.getName(), slug);
        return convertToResponseWithChildren(category);
    }

    @Cacheable(value = "categories", key = "'children_' + #parentId")
    public List<CategoryResponse> getCategoryChildren(Long parentId) {
        log.debug("Fetching children for category: {}", parentId);

        // Verify parent category exists
        categoryRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with ID: " + parentId));

        List<Category> children = categoryRepository.findByParentIdAndActiveTrue(parentId);
        
        log.debug("Found {} children for category: {}", children.size(), parentId);
        return children.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public Page<CategoryResponse> searchCategories(String keyword, int page, int size) {
        log.debug("Searching categories with keyword: '{}' - page: {}, size: {}", keyword, page, size);

        if (keyword == null || keyword.trim().isEmpty()) {
            throw new ValidationException("Search keyword cannot be empty");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<Category> categories = categoryRepository.searchByKeyword(keyword.trim(), pageable);

        log.debug("Found {} categories matching keyword '{}' out of {} total", 
                categories.getNumberOfElements(), keyword, categories.getTotalElements());

        return categories.map(this::convertToResponse);
    }

    // Category management methods

    @CacheEvict(value = {"categories", "category"}, allEntries = true)
    public CategoryResponse createCategory(CategoryRequest request) {
        log.info("Creating new category: {}", request.getName());

        // Validate unique name
        if (categoryRepository.existsByName(request.getName())) {
            log.warn("Category creation failed - name already exists: {}", request.getName());
            throw new ValidationException("Category with name '" + request.getName() + "' already exists");
        }

        // Validate parent category if provided
        Category parentCategory = null;
        if (request.getParentId() != null) {
            parentCategory = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with ID: " + request.getParentId()));
            
            // Prevent deep nesting (limit to 3 levels for example)
            if (getDepth(parentCategory) >= 3) {
                throw new ValidationException("Category nesting is limited to 3 levels");
            }
        }

        Category category = new Category();
        updateCategoryFromRequest(category, request);
        category.setParent(parentCategory);
        
        // Generate unique slug
        category.setSlug(generateUniqueSlug(request.getName()));

        category = categoryRepository.save(category);
        log.info("Category created successfully with ID: {} and slug: {}", category.getId(), category.getSlug());

        return convertToResponse(category);
    }

    @CachePut(value = "category", key = "#id")
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        log.info("Updating category: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

        // Check if name is being changed and if new name already exists
        if (!category.getName().equals(request.getName()) && 
            categoryRepository.existsByName(request.getName())) {
            log.warn("Category update failed - name already exists: {}", request.getName());
            throw new ValidationException("Category with name '" + request.getName() + "' already exists");
        }

        // Validate parent change
        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new ValidationException("Category cannot be its own parent");
            }
            
            Category newParent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with ID: " + request.getParentId()));
            
            // Check for circular reference
            if (isDescendant(category, newParent)) {
                throw new ValidationException("Cannot set parent to a descendant category (circular reference)");
            }
            
            category.setParent(newParent);
        } else {
            category.setParent(null);
        }

        String oldName = category.getName();
        updateCategoryFromRequest(category, request);

        // Update slug if name changed
        if (!oldName.equals(request.getName())) {
            category.setSlug(generateUniqueSlug(request.getName(), id));
        }

        category = categoryRepository.save(category);
        log.info("Category {} updated successfully", id);

        return convertToResponse(category);
    }

    @CacheEvict(value = {"categories", "category"}, allEntries = true)
    public void deleteCategory(Long id) {
        log.warn("Deleting category: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

        // Check if category has child categories
        long childCount = categoryRepository.countChildrenByParentId(id);
        if (childCount > 0) {
            log.warn("Category deletion failed - has {} child categories", childCount);
            throw new ValidationException(
                "Cannot delete category. It has " + childCount + " child categories. " +
                "Please move or delete child categories first.");
        }

        // Check if category has associated products
        long productCount = productRepository.countByCategoryIdAndStatus(id, Product.ProductStatus.ACTIVE);
        if (productCount > 0) {
            log.warn("Category deletion failed - has {} active products", productCount);
            throw new ValidationException(
                    "Cannot delete category. It has " + productCount + " active products associated with it. " +
                    "Please reassign or remove these products first.");
            }

            // Soft delete - set as inactive
            category.setActive(false);
            categoryRepository.save(category);

            log.warn("Category {} soft deleted (deactivated)", id);
        }

        @CachePut(value = "category", key = "#id")
        @CacheEvict(value = "categories", allEntries = true)
        public CategoryResponse updateCategoryStatus(Long id, boolean active) {
            log.info("Updating category {} status to: {}", id, active);

            Category category = categoryRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

            category.setActive(active);
            
            // If deactivating, also deactivate all child categories
            if (!active) {
                deactivateChildCategories(category);
            }

            category = categoryRepository.save(category);
            log.info("Category {} status updated to: {}", id, active);
            
            return convertToResponse(category);
        }

        @CachePut(value = "category", key = "#id")
        @CacheEvict(value = "categories", allEntries = true)
        public CategoryResponse updateFeaturedStatus(Long id, boolean featured) {
            log.info("Updating category {} featured status to: {}", id, featured);

            Category category = categoryRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

            category.setFeatured(featured);
            category = categoryRepository.save(category);

            log.info("Category {} featured status updated to: {}", id, featured);
            return convertToResponse(category);
        }

        @CachePut(value = "category", key = "#id")
        @CacheEvict(value = "categories", allEntries = true)
        public CategoryResponse updateSortOrder(Long id, Integer sortOrder) {
            log.info("Updating category {} sort order to: {}", id, sortOrder);

            Category category = categoryRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

            category.setSortOrder(sortOrder);
            category = categoryRepository.save(category);

            log.info("Category {} sort order updated to: {}", id, sortOrder);
            return convertToResponse(category);
        }

        @CachePut(value = "category", key = "#id")
        @CacheEvict(value = "categories", allEntries = true)
        public CategoryResponse updateParent(Long id, Long parentId) {
            log.info("Updating category {} parent to: {}", id, parentId);

            Category category = categoryRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

            Category newParent = null;
            if (parentId != null) {
                if (parentId.equals(id)) {
                    throw new ValidationException("Category cannot be its own parent");
                }

                newParent = categoryRepository.findById(parentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with ID: " + parentId));

                // Check for circular reference
                if (isDescendant(category, newParent)) {
                    throw new ValidationException("Cannot set parent to a descendant category (circular reference)");
                }

                // Check depth limit
                if (getDepth(newParent) >= 3) {
                    throw new ValidationException("Category nesting is limited to 3 levels");
                }
            }

            category.setParent(newParent);
            category = categoryRepository.save(category);

            log.info("Category {} parent updated to: {}", id, parentId);
            return convertToResponse(category);
        }

        // Statistics and analytics methods

        @Cacheable(value = "categoryStats", key = "'statistics'")
        public CategoryStatisticsResponse getCategoryStatistics() {
            log.debug("Generating category statistics");

            long totalCategories = categoryRepository.count();
            long activeCategories = categoryRepository.findByActiveTrue().size();
            long rootCategories = categoryRepository.countRootCategories();
            long featuredCategories = categoryRepository.findByFeaturedTrueAndActiveTrue().size();

            // Get categories with products
            List<Category> categoriesWithProducts = categoryRepository.findCategoriesWithProducts();
            List<CategoryStatisticsResponse.CategoryProductCount> topCategoriesByProducts = 
                    categoriesWithProducts.stream()
                            .limit(10)
                            .map(category -> CategoryStatisticsResponse.CategoryProductCount.builder()
                                    .categoryId(category.getId())
                                    .categoryName(category.getName())
                                    .productCount(category.getProductCount())
                                    .build())
                            .collect(Collectors.toList());

            // Calculate max depth
            int maxDepth = categoryRepository.findAll().stream()
                    .mapToInt(this::getDepth)
                    .max()
                    .orElse(0);

            CategoryStatisticsResponse statistics = CategoryStatisticsResponse.builder()
                    .totalCategories(totalCategories)
                    .activeCategories(activeCategories)
                    .rootCategories(rootCategories)
                    .featuredCategories(featuredCategories)
                    .topCategoriesByProducts(topCategoriesByProducts)
                    .maxDepth(maxDepth)
                    .build();

            log.debug("Category statistics generated - Total: {}, Active: {}, Root: {}, Featured: {}", 
                    totalCategories, activeCategories, rootCategories, featuredCategories);

            return statistics;
        }

        // Category-specific business methods

        public List<CategoryResponse> getCategoriesWithProducts() {
            log.debug("Fetching categories that have products");

            List<Category> categories = categoryRepository.findCategoriesWithProducts();
            
            log.debug("Found {} categories with products", categories.size());
            return categories.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        }

        public List<CategoryResponse> getCategoryPath(Long categoryId) {
            log.debug("Fetching category path for: {}", categoryId);

            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));

            List<Category> path = category.getAllParents();
            path.add(category);

            log.debug("Category path has {} levels", path.size());
            return path.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        }

        public List<CategoryResponse> getCategoryBreadcrumbs(Long categoryId) {
            log.debug("Fetching breadcrumbs for category: {}", categoryId);
            return getCategoryPath(categoryId);
        }

        public List<CategoryResponse> getCategoryAncestors(Long categoryId) {
            log.debug("Fetching ancestors for category: {}", categoryId);

            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));

            List<Category> ancestors = category.getAllParents();
            
            log.debug("Found {} ancestors for category: {}", ancestors.size(), categoryId);
            return ancestors.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        }

        public List<CategoryResponse> getCategoryDescendants(Long categoryId) {
            log.debug("Fetching all descendants for category: {}", categoryId);

            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));

            List<Category> descendants = category.getAllChildren();
            
            log.debug("Found {} descendants for category: {}", descendants.size(), categoryId);
            return descendants.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        }

        @Transactional
        public void updateCategoryProductCounts() {
            log.info("Updating category product counts");

            List<Category> allCategories = categoryRepository.findAll();
            int updatedCount = 0;

            for (Category category : allCategories) {
                long productCount = productRepository.countByCategoryIdAndStatus(
                        category.getId(), Product.ProductStatus.ACTIVE);
                
                if (!Objects.equals(productCount,category.getProductCount())) {
                    categoryRepository.updateProductCount(category.getId(), productCount);
                    updatedCount++;
                }
            }

            log.info("Updated product count for {} categories", updatedCount);
        }

        // Bulk operations

        @CacheEvict(value = {"categories", "category"}, allEntries = true)
        @Transactional
        public void bulkUpdateCategoryStatus(List<Long> categoryIds, boolean active) {
            log.info("Bulk updating status for {} categories to: {}", categoryIds.size(), active);

            List<Category> categories = categoryRepository.findAllById(categoryIds);
            if (categories.size() != categoryIds.size()) {
                throw new ValidationException("Some category IDs were not found");
            }

            categories.forEach(category -> {
                category.setActive(active);
                if (!active) {
                    deactivateChildCategories(category);
                }
            });
            
            categoryRepository.saveAll(categories);
            log.info("Successfully updated status for {} categories", categories.size());
        }

        @CacheEvict(value = {"categories", "category"}, allEntries = true)
        @Transactional
        public void bulkUpdateFeaturedStatus(List<Long> categoryIds, boolean featured) {
            log.info("Bulk updating featured status for {} categories to: {}", categoryIds.size(), featured);

            List<Category> categories = categoryRepository.findAllById(categoryIds);
            if (categories.size() != categoryIds.size()) {
                throw new ValidationException("Some category IDs were not found");
            }

            categories.forEach(category -> category.setFeatured(featured));
            categoryRepository.saveAll(categories);

            log.info("Successfully updated featured status for {} categories", categories.size());
        }

        @CacheEvict(value = {"categories", "category"}, allEntries = true)
        @Transactional
        public void bulkMoveCategories(List<Long> categoryIds, Long newParentId) {
            log.info("Bulk moving {} categories to parent: {}", categoryIds.size(), newParentId);

            Category tempParent = null;
            if (newParentId != null) {
            	tempParent = categoryRepository.findById(newParentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with ID: " + newParentId));
            }
            final Category newParent = tempParent;
            List<Category> categories = categoryRepository.findAllById(categoryIds);
            if (categories.size() != categoryIds.size()) {
                throw new ValidationException("Some category IDs were not found");
            }

            // Validate moves
            for (Category category : categories) {
                if (newParentId != null) {
                    if (newParentId.equals(category.getId())) {
                        throw new ValidationException("Category cannot be its own parent: " + category.getName());
                    }
                    if (isDescendant(category, newParent)) {
                        throw new ValidationException("Cannot move category to its descendant: " + category.getName());
                    }
                }
            }

            categories.forEach(category -> category.setParent(newParent));
            categoryRepository.saveAll(categories);

            log.info("Successfully moved {} categories", categories.size());
        }

        // Category hierarchy management

        @Transactional
        public void reorderCategories(List<Long> categoryIds, List<Integer> sortOrders) {
            log.info("Reordering {} categories", categoryIds.size());

            if (categoryIds.size() != sortOrders.size()) {
                throw new ValidationException("Category IDs and sort orders must have the same size");
            }

            List<Category> categories = categoryRepository.findAllById(categoryIds);
            if (categories.size() != categoryIds.size()) {
                throw new ValidationException("Some category IDs were not found");
            }

            Map<Long, Integer> orderMap = new HashMap<>();
            for (int i = 0; i < categoryIds.size(); i++) {
                orderMap.put(categoryIds.get(i), sortOrders.get(i));
            }

            categories.forEach(category -> {
                Integer newOrder = orderMap.get(category.getId());
                category.setSortOrder(newOrder != null ? newOrder : 0);
            });

            categoryRepository.saveAll(categories);
            log.info("Successfully reordered {} categories", categories.size());
        }

        // Private helper methods

        private void updateCategoryFromRequest(Category category, CategoryRequest request) {
            category.setName(request.getName());
            category.setDescription(request.getDescription());
            category.setImageUrl(request.getImageUrl());
            category.setIconUrl(request.getIconUrl());
            category.setActive(request.isActive());
            category.setFeatured(request.isFeatured());
            category.setSortOrder(request.getSortOrder());
            category.setMetaTitle(request.getMetaTitle());
            category.setMetaDescription(request.getMetaDescription());
            category.setMetaKeywords(request.getMetaKeywords());
        }

        private String generateUniqueSlug(String name) {
            return generateUniqueSlug(name, null);
        }

        private String generateUniqueSlug(String name, Long excludeId) {
            String baseSlug = SlugUtils.createSlug(name);
            String uniqueSlug = baseSlug;
            int counter = 1;

            while (true) {
                if (excludeId != null) {
                    Category existingCategory = categoryRepository.findBySlug(uniqueSlug).orElse(null);
                    if (existingCategory == null || existingCategory.getId().equals(excludeId)) {
                        break;
                    }
                } else {
                    if (!categoryRepository.existsBySlug(uniqueSlug)) {
                        break;
                    }
                }
                uniqueSlug = baseSlug + "-" + counter++;
            }

            return uniqueSlug;
        }

        private void deactivateChildCategories(Category category) {
            List<Category> children = category.getChildren();
            for (Category child : children) {
                child.setActive(false);
                deactivateChildCategories(child);
            }
            if (!children.isEmpty()) {
                categoryRepository.saveAll(children);
            }
        }

        private boolean isDescendant(Category ancestor, Category potentialDescendant) {
            if (potentialDescendant == null || ancestor == null) {
                return false;
            }
            
            Category current = potentialDescendant.getParent();
            while (current != null) {
                if (current.getId().equals(ancestor.getId())) {
                    return true;
                }
                current = current.getParent();
            }
            return false;
        }

        private int getDepth(Category category) {
            int depth = 0;
            Category current = category.getParent();
            while (current != null) {
                depth++;
                current = current.getParent();
            }
            return depth;
        }

        private CategoryResponse convertToResponse(Category category) {
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
                    .metaTitle(category.getMetaTitle())
                    .metaDescription(category.getMetaDescription())
                    .metaKeywords(category.getMetaKeywords())
                    .productCount(category.getProductCount())
                    .createdAt(category.getCreatedAt())
                    .updatedAt(category.getUpdatedAt())
                    .parent(category.getParent() != null ? convertToResponse(category.getParent()) : null)
                    .hasChildren(category.hasChildren())
                    .fullPath(category.getFullPath())
                    .depth(getDepth(category))
                    .breadcrumbs(category.getAllParents().stream()
                            .map(this::convertToResponse)
                            .collect(Collectors.toList()))
                    .build();
        }

        private CategoryResponse convertToResponseWithChildren(Category category) {
            CategoryResponse response = convertToResponse(category);
            
            List<CategoryResponse> children = category.getChildren().stream()
                    .filter(Category::isActive)
                    .sorted(Comparator.comparing(Category::getSortOrder)
                            .thenComparing(Category::getName))
                    .map(this::convertToResponseWithChildren)
                    .collect(Collectors.toList());
            
            response.setChildren(children);
            return response;
        }

        // Validation methods

        public void validateCategoryData(CategoryRequest request) {
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                throw new ValidationException("Category name is required");
            }

            if (request.getName().length() > 255) {
                throw new ValidationException("Category name cannot exceed 255 characters");
            }

            if (request.getDescription() != null && request.getDescription().length() > 1000) {
                throw new ValidationException("Category description cannot exceed 1000 characters");
            }

            if (request.getSortOrder() != null && request.getSortOrder() < 0) {
                throw new ValidationException("Sort order cannot be negative");
            }

            if (request.getParentId() != null) {
                Category parent = categoryRepository.findById(request.getParentId()).orElse(null);
                if (parent == null) {
                    throw new ValidationException("Parent category not found with ID: " + request.getParentId());
                }
                
                if (getDepth(parent) >= 3) {
                    throw new ValidationException("Category nesting is limited to 3 levels");
                }
            }
        }

        // Cache management methods

        @CacheEvict(value = {"categories", "category", "categoryStats"}, allEntries = true)
        public void clearCategoryCache() {
            log.info("Clearing all category caches");
        }

        @CacheEvict(value = "category", key = "#id")
        public void evictCategoryFromCache(Long id) {
            log.debug("Evicting category {} from cache", id);
        }

        // Advanced category methods

        public List<CategoryResponse> getSiblingCategories(Long categoryId) {
            log.debug("Fetching sibling categories for: {}", categoryId);

            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));

            List<Category> siblings;
            if (category.getParent() != null) {
                siblings = categoryRepository.findByParentIdAndActiveTrue(category.getParent().getId());
                siblings.removeIf(sibling -> sibling.getId().equals(categoryId));
            } else {
                siblings = categoryRepository.findByParentIsNullAndActiveTrue();
                siblings.removeIf(sibling -> sibling.getId().equals(categoryId));
            }

            log.debug("Found {} sibling categories for: {}", siblings.size(), categoryId);
            return siblings.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        }

        public Map<String, Object> getCategoryAnalytics(Long categoryId) {
            log.debug("Generating analytics for category: {}", categoryId);

            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));

            Map<String, Object> analytics = new HashMap<>();
            analytics.put("categoryId", categoryId);
            analytics.put("categoryName", category.getName());
            analytics.put("productCount", category.getProductCount());
            analytics.put("childrenCount", category.getChildren().size());
            analytics.put("depth", getDepth(category));
            analytics.put("isRoot", category.isRootCategory());
            analytics.put("isLeaf", !category.hasChildren());
            analytics.put("fullPath", category.getFullPath());

            // Additional analytics could include:
            // - Average product price in category
            // - Most popular products
            // - Sales data
            // - View statistics

            log.debug("Generated analytics for category: {}", categoryId);
            return analytics;
        }

        @Transactional
        public CategoryResponse duplicateCategory(Long categoryId, String newName, Long newParentId) {
            log.info("Duplicating category: {} with new name: {}", categoryId, newName);

            Category originalCategory = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));

            if (categoryRepository.existsByName(newName)) {
                throw new ValidationException("Category with name '" + newName + "' already exists");
            }

            Category newParent = null;
            if (newParentId != null) {
                newParent = categoryRepository.findById(newParentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with ID: " + newParentId));
            }

            Category duplicatedCategory = new Category();
            duplicatedCategory.setName(newName);
            duplicatedCategory.setDescription(originalCategory.getDescription());
            duplicatedCategory.setImageUrl(originalCategory.getImageUrl());
            duplicatedCategory.setIconUrl(originalCategory.getIconUrl());
            duplicatedCategory.setActive(false); // Start as inactive
            duplicatedCategory.setFeatured(false);
            duplicatedCategory.setSortOrder(originalCategory.getSortOrder());
            duplicatedCategory.setMetaTitle(originalCategory.getMetaTitle());
            duplicatedCategory.setMetaDescription(originalCategory.getMetaDescription());
            duplicatedCategory.setMetaKeywords(originalCategory.getMetaKeywords());
            duplicatedCategory.setParent(newParent);
            duplicatedCategory.setSlug(generateUniqueSlug(newName));

            duplicatedCategory = categoryRepository.save(duplicatedCategory);
            
            log.info("Category duplicated successfully with ID: {}", duplicatedCategory.getId());
            return convertToResponse(duplicatedCategory);
        }

        public List<CategoryResponse> findCategoriesWithNoProducts() {
            log.debug("Fetching categories with no products");

            List<Category> categories = categoryRepository.findAll().stream()
                    .filter(category -> category.getProductCount() == 0)
                    .collect(Collectors.toList());

            log.debug("Found {} categories with no products", categories.size());
            return categories.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        }
    }
