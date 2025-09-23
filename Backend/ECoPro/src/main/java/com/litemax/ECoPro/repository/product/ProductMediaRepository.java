package com.litemax.ECoPro.repository.product;

import com.litemax.ECoPro.entity.product.ProductMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductMediaRepository extends JpaRepository<ProductMedia, Long> {

    List<ProductMedia> findByProductIdOrderBySortOrder(Long productId);
    List<ProductMedia> findByProductId(Long productId);
    
    Optional<ProductMedia> findByProductIdAndPrimaryTrue(Long productId);
    
    List<ProductMedia> findByProductIdAndType(Long productId, ProductMedia.MediaType type);
    
    @Modifying
    @Query("UPDATE ProductMedia pm SET pm.primary = false WHERE pm.product.id = :productId")
    void resetPrimaryForProduct(@Param("productId") Long productId);
    
    @Modifying
    @Query("DELETE FROM ProductMedia pm WHERE pm.product.id = :productId")
    void deleteByProductId(@Param("productId") Long productId);
    
    long countByProductId(Long productId);
}