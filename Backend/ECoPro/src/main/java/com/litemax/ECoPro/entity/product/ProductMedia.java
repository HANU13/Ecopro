package com.litemax.ECoPro.entity.product;

import jakarta.persistence.*;

@Entity
@Table(name = "product_media")
public class ProductMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private String url;
    private Integer sortOrder;
    private String altText;

    // Getters and Setters
}
