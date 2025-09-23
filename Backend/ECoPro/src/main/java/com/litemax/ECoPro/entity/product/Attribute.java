package com.litemax.ECoPro.entity.product;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "attributes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Attribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttributeType type = AttributeType.TEXT;

    @Column(name = "is_required")
    private boolean required = false;

    @Column(name = "is_filterable")
    private boolean filterable = false;

    @Column(name = "is_searchable")
    private boolean searchable = false;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "attribute", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AttributeValue> values = new ArrayList<>();

    @OneToMany(mappedBy = "attribute", cascade = CascadeType.ALL)
    private List<ProductAttribute> productAttributes = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public enum AttributeType {
        TEXT, NUMBER, BOOLEAN, SELECT, MULTISELECT, DATE, COLOR, URL, EMAIL
    }
}
