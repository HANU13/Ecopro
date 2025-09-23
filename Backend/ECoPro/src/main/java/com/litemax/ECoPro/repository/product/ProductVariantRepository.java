package com.litemax.ECoPro.repository.product;

import com.litemax.ECoPro.entity.product.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductIdAndActiveTrue(Long productId);
    List<ProductVariant> findByProductIdOrderBySortOrder(Long productId);
    List<ProductVariant> findByProductId(Long productId);
    
    Optional<ProductVariant> findBySku(String sku);
    boolean existsBySku(String sku);
    
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.product.id = :productId AND pv.inventoryQuantory > 0")
    List<ProductVariant> findInStockVariantsByProductId(@Param("productId") Long productId);
    
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.inventoryQuantory <= 0 AND pv.active = true")
    List<ProductVariant> findOutOfStockVariants();
    
    long countByProductId(Long productId);
}