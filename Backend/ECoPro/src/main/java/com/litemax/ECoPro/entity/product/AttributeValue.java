package com.litemax.ECoPro.entity.product;

import jakarta.persistence.*;
import java.util.Set;

@Entity
@Table(name = "attribute_values")
public class AttributeValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "attribute_id", nullable = false)
    private Attribute attribute;

    private String value;

    @ManyToMany(mappedBy = "attributeValues")
    private Set<ProductVariant> variants;

    // Getters and Setters
}
