package com.litemax.ECoPro.entity.auth;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "role_permissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many RolePermissions to one Role
    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    // Many RolePermissions to one Permission
    @ManyToOne
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;
}
