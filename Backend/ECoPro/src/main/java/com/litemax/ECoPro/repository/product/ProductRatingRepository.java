package com.litemax.ECoPro.repository.product;

import com.litemax.ECoPro.entity.product.ProductRating;
import com.litemax.ECoPro.entity.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRatingRepository extends JpaRepository<ProductRating, Long> {
    
    Optional<ProductRating> findByProduct(Product product);
    
    void deleteByProduct(Product product);
}