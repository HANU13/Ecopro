package com.litemax.ECoPro.entity.product;

import jakarta.persistence.*;
import java.util.Set;

@Entity
@Table(name = "attributes")
public class Attribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    private String type; // e.g., text, number, dropdown

    @OneToMany(mappedBy = "attribute")
    private Set<AttributeValue> values;

    // Getters and Setters
}
