package com.litemax.ECoPro.entity.auth;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "permissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    private PermissionCategory category;

    public enum PermissionCategory {
        USER_MANAGEMENT,
        PRODUCT_MANAGEMENT,
        ORDER_MANAGEMENT,
        PAYMENT_MANAGEMENT,
        INVENTORY_MANAGEMENT,
        REPORTING,
        SYSTEM_ADMIN
    }
}