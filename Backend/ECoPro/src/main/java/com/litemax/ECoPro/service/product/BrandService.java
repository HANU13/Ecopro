package com.litemax.ECoPro.service.product;

import com.litemax.ECoPro.dto.product.BrandRequest;
import com.litemax.ECoPro.dto.product.BrandResponse;
import com.litemax.ECoPro.dto.product.BrandStatisticsResponse;
import com.litemax.ECoPro.entity.product.Brand;
import com.litemax.ECoPro.entity.product.Product;
import com.litemax.ECoPro.exception.ResourceNotFoundException;
import com.litemax.ECoPro.exception.ValidationException;
import com.litemax.ECoPro.repository.product.BrandRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BrandService {

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;

    // Public browsing methods

    @Cacheable(value = "brands", key = "'all_brands_' + #page + '_' + #size + '_' + #includeInactive")
    public Page<BrandResponse> getAllBrands(int page, int size, boolean includeInactive) {
        log.debug("Fetching all brands - page: {}, size: {}, includeInactive: {}", page, size, includeInactive);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        
        Page<Brand> brands;
        if (includeInactive) {
            brands = brandRepository.findAll(pageable);
        } else {
            brands = brandRepository.findByActiveTrue(pageable);
        }

        log.debug("Found {} brands out of {} total", brands.getNumberOfElements(), brands.getTotalElements());
        return brands.map(this::convertToResponse);
    }

    @Cacheable(value = "brands", key = "'active_brands_list'")
    public List<BrandResponse> getActiveBrands() {
        log.debug("Fetching active brands list");
        
        List<Brand> brands = brandRepository.findByActiveTrueOrderBySortOrder();
        
        log.debug("Found {} active brands", brands.size());
        return brands.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "brands", key = "'featured_brands'")
    public List<BrandResponse> getFeaturedBrands() {
        log.debug("Fetching featured brands");
        
        List<Brand> featuredBrands = brandRepository.findByFeaturedTrueAndActiveTrueOrderBySortOrder();
        
        log.debug("Found {} featured brands", featuredBrands.size());
        return featuredBrands.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "brands", key = "'popular_brands_' + #page + '_' + #size")
    public Page<BrandResponse> getPopularBrands(int page, int size) {
        log.debug("Fetching popular brands - page: {}, size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Brand> popularBrands = brandRepository.findPopularBrands(pageable);
        
        log.debug("Found {} popular brands out of {} total", 
                popularBrands.getNumberOfElements(), popularBrands.getTotalElements());
        
        return popularBrands.map(this::convertToResponse);
    }

    @Cacheable(value = "brand", key = "#id")
    public BrandResponse getBrandById(Long id) {
        log.debug("Fetching brand by ID: {}", id);
        
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with ID: " + id));
        
        log.debug("Found brand: {} (ID: {})", brand.getName(), id);
        return convertToResponse(brand);
    }

    @Cacheable(value = "brand", key = "'slug_' + #slug")
    public BrandResponse getBrandBySlug(String slug) {
        log.debug("Fetching brand by slug: {}", slug);
        
        Brand brand = brandRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with slug: " + slug));
        
        log.debug("Found brand: {} (slug: {})", brand.getName(), slug);
        return convertToResponse(brand);
    }

    public Page<BrandResponse> searchBrands(String keyword, int page, int size) {
        log.debug("Searching brands with keyword: '{}' - page: {}, size: {}", keyword, page, size);
        
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new ValidationException("Search keyword cannot be empty");
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<Brand> brands = brandRepository.searchByKeyword(keyword.trim(), pageable);
        
        log.debug("Found {} brands matching keyword '{}' out of {} total", 
                brands.getNumberOfElements(), keyword, brands.getTotalElements());
        
        return brands.map(this::convertToResponse);
    }

    // Brand management methods

    @CacheEvict(value = {"brands", "brand"}, allEntries = true)
    public BrandResponse createBrand(BrandRequest request) {
        log.info("Creating new brand: {}", request.getName());

        // Validate unique name
        if (brandRepository.existsByName(request.getName())) {
            log.warn("Brand creation failed - name already exists: {}", request.getName());
            throw new ValidationException("Brand with name '" + request.getName() + "' already exists");
        }

        Brand brand = new Brand();
        updateBrandFromRequest(brand, request);
        
        // Generate unique slug
        brand.setSlug(generateUniqueSlug(request.getName()));
        
        brand = brandRepository.save(brand);
        log.info("Brand created successfully with ID: {} and slug: {}", brand.getId(), brand.getSlug());

        return convertToResponse(brand);
    }

    @CachePut(value = "brand", key = "#id")
    @CacheEvict(value = "brands", allEntries = true)
    public BrandResponse updateBrand(Long id, BrandRequest request) {
        log.info("Updating brand: {}", id);

        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with ID: " + id));

        // Check if name is being changed and if new name already exists
        if (!brand.getName().equals(request.getName()) && 
            brandRepository.existsByName(request.getName())) {
            log.warn("Brand update failed - name already exists: {}", request.getName());
            throw new ValidationException("Brand with name '" + request.getName() + "' already exists");
        }

        String oldName = brand.getName();
        updateBrandFromRequest(brand, request);

        // Update slug if name changed
        if (!oldName.equals(request.getName())) {
            brand.setSlug(generateUniqueSlug(request.getName(), id));
        }

        brand = brandRepository.save(brand);
        log.info("Brand {} updated successfully", id);

        return convertToResponse(brand);
    }

    @CacheEvict(value = {"brands", "brand"}, allEntries = true)
    public void deleteBrand(Long id) {
        log.warn("Deleting brand: {}", id);

        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with ID: " + id));

        // Check if brand has associated products
        long productCount = productRepository.countByBrandIdAndStatus(
                id, Product.ProductStatus.ACTIVE);
        if (productCount > 0) {
            log.warn("Brand deletion failed - has {} active products", productCount);
            throw new ValidationException(
                "Cannot delete brand. It has " + productCount + " active products associated with it. " +
                "Please reassign or remove these products first.");
        }

        // Soft delete - set as inactive
        brand.setActive(false);
        brandRepository.save(brand);

        log.warn("Brand {} soft deleted (deactivated)", id);
    }

    @CachePut(value = "brand", key = "#id")
    @CacheEvict(value = "brands", allEntries = true)
    public BrandResponse updateBrandStatus(Long id, boolean active) {
        log.info("Updating brand {} status to: {}", id, active);

        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with ID: " + id));

        brand.setActive(active);
        brand = brandRepository.save(brand);

        log.info("Brand {} status updated to: {}", id, active);
        return convertToResponse(brand);
    }

    @CachePut(value = "brand", key = "#id")
    @CacheEvict(value = "brands", allEntries = true)
    public BrandResponse updateFeaturedStatus(Long id, boolean featured) {
        log.info("Updating brand {} featured status to: {}", id, featured);

        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with ID: " + id));

        brand.setFeatured(featured);
        brand = brandRepository.save(brand);

        log.info("Brand {} featured status updated to: {}", id, featured);
        return convertToResponse(brand);
    }

    @CachePut(value = "brand", key = "#id")
    @CacheEvict(value = "brands", allEntries = true)
    public BrandResponse updateSortOrder(Long id, Integer sortOrder) {
        log.info("Updating brand {} sort order to: {}", id, sortOrder);

        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with ID: " + id));

        brand.setSortOrder(sortOrder);
        brand = brandRepository.save(brand);

        log.info("Brand {} sort order updated to: {}", id, sortOrder);
        return convertToResponse(brand);
    }

    // Statistics and analytics methods

    @Cacheable(value = "brandStats", key = "'statistics'")
    public BrandStatisticsResponse getBrandStatistics() {
        log.debug("Generating brand statistics");

        long totalBrands = brandRepository.count();
        long activeBrands = brandRepository.findByActiveTrue().size();
        long featuredBrands = brandRepository.findByFeaturedTrueAndActiveTrue().size();

        // Get top brands by product count
        List<Brand> topBrands = brandRepository.findPopularBrands();
        List<BrandStatisticsResponse.BrandProductCount> topBrandsByProducts = topBrands.stream()
                .limit(10)
                .map(brand -> BrandStatisticsResponse.BrandProductCount.builder()
                        .brandId(brand.getId())
                        .brandName(brand.getName())
                        .productCount(brand.getProductCount())
                        .build())
                .collect(Collectors.toList());

        // Get top countries (this would need additional logic based on your requirements)
        List<String> topCountries = brandRepository.findAll().stream()
                .filter(brand -> brand.getOriginCountry() != null)
                .collect(Collectors.groupingBy(Brand::getOriginCountry, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        BrandStatisticsResponse statistics = BrandStatisticsResponse.builder()
                .totalBrands(totalBrands)
                .activeBrands(activeBrands)
                .featuredBrands(featuredBrands)
                .topBrandsByProducts(topBrandsByProducts)
                .topCountries(topCountries)
                .build();

        log.debug("Brand statistics generated - Total: {}, Active: {}, Featured: {}", 
                totalBrands, activeBrands, featuredBrands);

        return statistics;
    }

    // Brand-specific business methods

    public List<BrandResponse> getBrandsByCountry(String country) {
        log.debug("Fetching brands by country: {}", country);

        List<Brand> brands = brandRepository.findAll().stream()
                .filter(brand -> brand.isActive() && 
                        country.equalsIgnoreCase(brand.getOriginCountry()))
                .sorted((b1, b2) -> b1.getName().compareToIgnoreCase(b2.getName()))
                .collect(Collectors.toList());

        log.debug("Found {} brands from country: {}", brands.size(), country);
        return brands.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<BrandResponse> getBrandsWithProducts() {
        log.debug("Fetching brands that have products");

        List<Brand> brands = brandRepository.findAll().stream()
                .filter(brand -> brand.isActive() && brand.getProductCount() > 0)
                .sorted((b1, b2) -> b2.getProductCount().compareTo(b1.getProductCount()))
                .collect(Collectors.toList());

        log.debug("Found {} brands with products", brands.size());
        return brands.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateBrandProductCounts() {
        log.info("Updating brand product counts");

        List<Brand> allBrands = brandRepository.findAll();
        int updatedCount = 0;

        for (Brand brand : allBrands) {
            long productCount = productRepository.countByBrandIdAndStatus(
                    brand.getId(), Product.ProductStatus.ACTIVE);
            
            if (!Objects.equals(productCount,brand.getProductCount())) {
                brand.setProductCount(productCount);
                brandRepository.save(brand);
                updatedCount++;
            }
        }

        log.info("Updated product count for {} brands", updatedCount);
    }

    // Bulk operations

    @CacheEvict(value = {"brands", "brand"}, allEntries = true)
    @Transactional
    public void bulkUpdateBrandStatus(List<Long> brandIds, boolean active) {
        log.info("Bulk updating status for {} brands to: {}", brandIds.size(), active);

        List<Brand> brands = brandRepository.findAllById(brandIds);
        if (brands.size() != brandIds.size()) {
            throw new ValidationException("Some brand IDs were not found");
        }

        brands.forEach(brand -> brand.setActive(active));
        brandRepository.saveAll(brands);

        log.info("Successfully updated status for {} brands", brands.size());
    }

    @CacheEvict(value = {"brands", "brand"}, allEntries = true)
    @Transactional
    public void bulkUpdateFeaturedStatus(List<Long> brandIds, boolean featured) {
        log.info("Bulk updating featured status for {} brands to: {}", brandIds.size(), featured);

        List<Brand> brands = brandRepository.findAllById(brandIds);
        if (brands.size() != brandIds.size()) {
            throw new ValidationException("Some brand IDs were not found");
        }

        brands.forEach(brand -> brand.setFeatured(featured));
        brandRepository.saveAll(brands);

        log.info("Successfully updated featured status for {} brands", brands.size());
    }

    // Private helper methods

    private void updateBrandFromRequest(Brand brand, BrandRequest request) {
        brand.setName(request.getName());
        brand.setDescription(request.getDescription());
        brand.setLogoUrl(request.getLogoUrl());
        brand.setBannerUrl(request.getBannerUrl());
        brand.setWebsiteUrl(request.getWebsiteUrl());
        brand.setActive(request.isActive());
        brand.setFeatured(request.isFeatured());
        brand.setSortOrder(request.getSortOrder());
        brand.setMetaTitle(request.getMetaTitle());
        brand.setMetaDescription(request.getMetaDescription());
        brand.setMetaKeywords(request.getMetaKeywords());
        brand.setOriginCountry(request.getOriginCountry());
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
                // For updates, exclude the current brand's slug
                Brand existingBrand = brandRepository.findBySlug(uniqueSlug).orElse(null);
                if (existingBrand == null || existingBrand.getId().equals(excludeId)) {
                    break;
                }
            } else {
                // For new brands
                if (!brandRepository.existsBySlug(uniqueSlug)) {
                    break;
                }
            }
            uniqueSlug = baseSlug + "-" + counter++;
        }

        return uniqueSlug;
    }

    private BrandResponse convertToResponse(Brand brand) {
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
                .metaTitle(brand.getMetaTitle())
                .metaDescription(brand.getMetaDescription())
                .metaKeywords(brand.getMetaKeywords())
                .productCount(brand.getProductCount())
                .originCountry(brand.getOriginCountry())
                .createdAt(brand.getCreatedAt())
                .updatedAt(brand.getUpdatedAt())
                .build();
    }

    // Validation methods

    public void validateBrandData(BrandRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new ValidationException("Brand name is required");
        }

        if (request.getName().length() > 255) {
            throw new ValidationException("Brand name cannot exceed 255 characters");
        }

        if (request.getDescription() != null && request.getDescription().length() > 1000) {
            throw new ValidationException("Brand description cannot exceed 1000 characters");
        }

        if (request.getWebsiteUrl() != null && !isValidUrl(request.getWebsiteUrl())) {
            throw new ValidationException("Invalid website URL format");
        }

        if (request.getSortOrder() != null && request.getSortOrder() < 0) {
            throw new ValidationException("Sort order cannot be negative");
        }
    }

    private boolean isValidUrl(String url) {
        try {
            new java.net.URL(url);
            return true;
        } catch (java.net.MalformedURLException e) {
            return false;
        }
    }

    // Cache management methods

    @CacheEvict(value = {"brands", "brand", "brandStats"}, allEntries = true)
    public void clearBrandCache() {
        log.info("Clearing all brand caches");
    }

    @CacheEvict(value = "brand", key = "#id")
    public void evictBrandFromCache(Long id) {
        log.debug("Evicting brand {} from cache", id);
    }
}