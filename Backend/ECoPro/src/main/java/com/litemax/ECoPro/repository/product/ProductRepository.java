package com.litemax.ECoPro.repository.product;

import com.litemax.ECoPro.entity.product.Product;
import com.litemax.ECoPro.entity.product.Product.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    // Basic queries
    Optional<Product> findBySlug(String slug);
    Optional<Product> findBySku(String sku);
    boolean existsBySlug(String slug);
    boolean existsBySku(String sku);

    // Status-based queries
    List<Product> findByStatus(ProductStatus status);
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);
    List<Product> findByStatusAndFeaturedTrue(ProductStatus status);

    // Seller-based queries
    List<Product> findBySellerIdAndStatus(Long sellerId, ProductStatus status);
    Page<Product> findBySellerIdAndStatus(Long sellerId, ProductStatus status, Pageable pageable);
    Page<Product> findBySellerId(Long sellerId, Pageable pageable);

    // Category-based queries
    @Query("SELECT p FROM Product p JOIN p.categories c WHERE c.id = :categoryId AND p.status = :status")
    Page<Product> findByCategoryIdAndStatus(@Param("categoryId") Long categoryId, 
                                           @Param("status") ProductStatus status, Pageable pageable);

    @Query("SELECT p FROM Product p JOIN p.categories c WHERE c.id IN :categoryIds AND p.status = :status")
    Page<Product> findByCategoryIdsAndStatus(@Param("categoryIds") List<Long> categoryIds, 
                                           @Param("status") ProductStatus status, Pageable pageable);

    // Brand-based queries
    Page<Product> findByBrandIdAndStatus(Long brandId, ProductStatus status, Pageable pageable);

    // Price-based queries
    @Query("SELECT p FROM Product p WHERE p.price BETWEEN :minPrice AND :maxPrice AND p.status = :status")
    Page<Product> findByPriceRange(@Param("minPrice") BigDecimal minPrice, 
                                  @Param("maxPrice") BigDecimal maxPrice, 
                                  @Param("status") ProductStatus status, Pageable pageable);

    // Search queries
    @Query("SELECT p FROM Product p WHERE " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.tags) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "p.status = :status")
    Page<Product> searchByKeywordAndStatus(@Param("keyword") String keyword, 
                                         @Param("status") ProductStatus status, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.tags) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // Featured and popular products
    List<Product> findByFeaturedTrueAndStatus(ProductStatus status);
    
    @Query("SELECT p FROM Product p WHERE p.status = :status ORDER BY p.viewCount DESC")
    Page<Product> findPopularProducts(@Param("status") ProductStatus status, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.status = :status ORDER BY p.createdAt DESC")
    Page<Product> findLatestProducts(@Param("status") ProductStatus status, Pageable pageable);

    // Rating and review queries
    @Query("SELECT p FROM Product p WHERE p.rating >= :minRating AND p.status = :status")
    Page<Product> findByMinRatingAndStatus(@Param("minRating") BigDecimal minRating, 
                                          @Param("status") ProductStatus status, Pageable pageable);

    // Inventory queries
    @Query("SELECT p FROM Product p WHERE p.inventoryQuantity <= p.lowStockThreshold AND p.trackInventory = true")
    List<Product> findLowStockProducts();

    @Query("SELECT p FROM Product p WHERE p.inventoryQuantity = 0 AND p.trackInventory = true")
    List<Product> findOutOfStockProducts();

    // Statistics queries
    @Query("SELECT COUNT(p) FROM Product p WHERE p.status = :status")
    long countByStatus(@Param("status") ProductStatus status);
    
    @Query("SELECT COUNT(p) FROM Product p WHERE p.brand.id = :brandId AND p.status = :status")
    long countByBrandIdAndStatus(@Param("brandId") Long brandId,@Param("status") ProductStatus status);
    
    @Query("SELECT COUNT(p) FROM Product p JOIN p.categories c "
    		+ "WHERE c.id = :categoryId AND p.status = :status")
    long countByCategoryIdAndStatus(@Param("categoryId") Long categoryId,@Param("status") ProductStatus status);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.seller.id = :sellerId AND p.status = :status")
    long countBySellerIdAndStatus(@Param("sellerId") Long sellerId, @Param("status") ProductStatus status);

    // Update queries
    @Modifying
    @Query("UPDATE Product p SET p.viewCount = p.viewCount + 1 WHERE p.id = :productId")
    void incrementViewCount(@Param("productId") Long productId);

    @Modifying
    @Query("UPDATE Product p SET p.rating = :rating, p.reviewCount = :reviewCount WHERE p.id = :productId")
    void updateRatingAndReviewCount(@Param("productId") Long productId, 
                                   @Param("rating") BigDecimal rating, 
                                   @Param("reviewCount") Integer reviewCount);

    // Complex filtering query
    @Query("SELECT p FROM Product p " +
            "LEFT JOIN p.categories c " +
            "LEFT JOIN p.brand b " +
            "WHERE (:categoryIds IS NULL OR c.id IN :categoryIds) " +
            "AND (:brandIds IS NULL OR b.id IN :brandIds) " +
            "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
            "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
            "AND (:minRating IS NULL OR p.rating >= :minRating) " +
            "AND p.status = :status " +
            "GROUP BY p.id")
    Page<Product> findProductsWithFilters(@Param("categoryIds") List<Long> categoryIds,
                                          @Param("brandIds") List<Long> brandIds,
                                          @Param("minPrice") BigDecimal minPrice,
                                          @Param("maxPrice") BigDecimal maxPrice,
                                          @Param("minRating") BigDecimal minRating,
                                          @Param("status") ProductStatus status,
                                          Pageable pageable);
}
