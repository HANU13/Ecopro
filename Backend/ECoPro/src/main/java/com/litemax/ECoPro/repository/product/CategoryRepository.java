package com.litemax.ECoPro.repository.product;

import com.litemax.ECoPro.entity.product.Category;
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
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Basic queries
    Optional<Category> findBySlug(String slug);
    Optional<Category> findByName(String name);
    boolean existsBySlug(String slug);
    boolean existsByName(String name);

    // Active categories
    List<Category> findByActiveTrue();
    Page<Category> findByActiveTrue(Pageable pageable);
    List<Category> findByActiveTrueOrderBySortOrder();

    // Parent-child relationships
    List<Category> findByParentIsNullAndActiveTrue(); // Root categories
    List<Category> findByParentIdAndActiveTrue(Long parentId);
    List<Category> findByParentId(Long parentId);
    
    @Query("SELECT c FROM Category c WHERE c.parent IS NULL ORDER BY c.sortOrder")
    List<Category> findRootCategoriesOrdered();

    // Featured categories
    List<Category> findByFeaturedTrueAndActiveTrue();
    List<Category> findByFeaturedTrueAndActiveTrueOrderBySortOrder();

    // Search
    @Query("SELECT c FROM Category c WHERE " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Category> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // Statistics
    @Query("SELECT COUNT(c) FROM Category c WHERE c.parent.id = :parentId AND c.active = true")
    long countChildrenByParentId(@Param("parentId") Long parentId);

    @Query("SELECT COUNT(c) FROM Category c WHERE c.parent IS NULL AND c.active = true")
    long countRootCategories();

    // Update queries
    @Modifying
    @Query("UPDATE Category c SET c.productCount = :count WHERE c.id = :categoryId")
    void updateProductCount(@Param("categoryId") Long categoryId, @Param("count") Long count);

    // Hierarchy queries
    @Query("SELECT c FROM Category c WHERE c.active = true ORDER BY c.parent.id NULLS FIRST, c.sortOrder")
    List<Category> findAllOrderedByHierarchy();

    // Categories with product count
    @Query("SELECT c FROM Category c WHERE c.productCount > 0 AND c.active = true ORDER BY c.productCount DESC")
    List<Category> findCategoriesWithProducts();
}