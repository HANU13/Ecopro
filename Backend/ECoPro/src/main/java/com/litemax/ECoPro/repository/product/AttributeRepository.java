package com.litemax.ECoPro.repository.product;

import com.litemax.ECoPro.entity.product.Attribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttributeRepository extends JpaRepository<Attribute, Long> {

    Optional<Attribute> findByCode(String code);
    Optional<Attribute> findByName(String name);
    boolean existsByCode(String code);
    boolean existsByName(String name);
    
    List<Attribute> findByFilterableTrueOrderBySortOrder();
    List<Attribute> findBySearchableTrueOrderBySortOrder();
    List<Attribute> findAllByOrderBySortOrder();
}