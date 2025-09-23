package com.litemax.ECoPro.repository.product;

import com.litemax.ECoPro.entity.product.Brand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {

    // Basic queries
    Optional<Brand> findBySlug(String slug);
    Optional<Brand> findByName(String name);
    boolean existsBySlug(String slug);
    boolean existsByName(String name);

    // Active brands
    List<Brand> findByActiveTrue();
    Page<Brand> findByActiveTrue(Pageable pageable);
    List<Brand> findByActiveTrueOrderBySortOrder();

    // Featured brands
    List<Brand> findByFeaturedTrueAndActiveTrue();
    List<Brand> findByFeaturedTrueAndActiveTrueOrderBySortOrder();

    // Search
    @Query("SELECT b FROM Brand b WHERE " +
           "LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(b.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Brand> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // Popular brands by product count
    @Query("SELECT b FROM Brand b WHERE b.productCount > 0 AND b.active = true ORDER BY b.productCount DESC")
    List<Brand> findPopularBrands();

    @Query("SELECT b FROM Brand b WHERE b.productCount > 0 AND b.active = true ORDER BY b.productCount DESC")
    Page<Brand> findPopularBrands(Pageable pageable);

    // Update queries
    @Modifying
    @Query("UPDATE Brand b SET b.productCount = :count WHERE b.id = :brandId")
    void updateProductCount(@Param("brandId") Long brandId, @Param("count") Long count);
}