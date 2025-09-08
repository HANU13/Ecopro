package com.litemax.ECoPro.entity.product;

import jakarta.persistence.*;

@Entity
@Table(name = "variant_attribute_values")
public class VariantAttributeValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @ManyToOne
    @JoinColumn(name = "attribute_value_id")
    private AttributeValue attributeValue;

    // Getters and Setters
}
