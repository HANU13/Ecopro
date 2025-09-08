package com.litemax.ECoPro.entity.product;

import jakarta.persistence.*;
import java.util.Set;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private Long parentId;
    private String path;

    @ManyToMany(mappedBy = "categories")
    private Set<Product> products;

    // Getters and Setters
}
