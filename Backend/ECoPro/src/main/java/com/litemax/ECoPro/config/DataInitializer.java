package com.litemax.ECoPro.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.litemax.ECoPro.entity.auth.Permission;
import com.litemax.ECoPro.entity.auth.Role;
import com.litemax.ECoPro.entity.auth.User;
import com.litemax.ECoPro.repository.auth.PermissionRepository;
import com.litemax.ECoPro.repository.auth.RoleRepository;
import com.litemax.ECoPro.repository.auth.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Initializing default data...");
        
        createPermissions();
        createRoles();
        createDefaultAdmin();
        
        log.info("Default data initialization completed");
    }

    private void createPermissions() {
        log.info("Creating default permissions...");
        
        List<Permission> permissions = Arrays.asList(
            // User Management
            createPermission("USER_READ", "Read user information", Permission.PermissionCategory.USER_MANAGEMENT),
            createPermission("USER_WRITE", "Create and update users", Permission.PermissionCategory.USER_MANAGEMENT),
            createPermission("USER_DELETE", "Delete users", Permission.PermissionCategory.USER_MANAGEMENT),
            
            // Product Management
            createPermission("PRODUCT_READ", "View products", Permission.PermissionCategory.PRODUCT_MANAGEMENT),
            createPermission("PRODUCT_WRITE", "Create and update products", Permission.PermissionCategory.PRODUCT_MANAGEMENT),
            createPermission("PRODUCT_DELETE", "Delete products", Permission.PermissionCategory.PRODUCT_MANAGEMENT),
            
            // Order Management
            createPermission("ORDER_READ", "View orders", Permission.PermissionCategory.ORDER_MANAGEMENT),
            createPermission("ORDER_WRITE", "Create and update orders", Permission.PermissionCategory.ORDER_MANAGEMENT),
            createPermission("ORDER_DELETE", "Cancel orders", Permission.PermissionCategory.ORDER_MANAGEMENT),
            
            // Payment Management
            createPermission("PAYMENT_READ", "View payments", Permission.PermissionCategory.PAYMENT_MANAGEMENT),
            createPermission("PAYMENT_WRITE", "Process payments", Permission.PermissionCategory.PAYMENT_MANAGEMENT),
            
            // Inventory Management
            createPermission("INVENTORY_READ", "View inventory", Permission.PermissionCategory.INVENTORY_MANAGEMENT),
            createPermission("INVENTORY_WRITE", "Manage inventory", Permission.PermissionCategory.INVENTORY_MANAGEMENT),
            
            // Reporting
            createPermission("REPORT_READ", "View reports", Permission.PermissionCategory.REPORTING),
            
            // System Admin
            createPermission("SYSTEM_ADMIN", "Full system access", Permission.PermissionCategory.SYSTEM_ADMIN)
        );

        permissions.forEach(permission -> {
            if (!permissionRepository.existsByName(permission.getName())) {
                permissionRepository.save(permission);
                log.debug("Created permission: {}", permission.getName());
            }
        });
    }

    private Permission createPermission(String name, String description, Permission.PermissionCategory category) {
        Permission permission = new Permission();
        permission.setName(name);
        permission.setDescription(description);
        permission.setCategory(category);
        return permission;
    }

    private void createRoles() {
        log.info("Creating default roles...");
        
        // Admin Role
        if (!roleRepository.existsByName("ADMIN")) {
            Role adminRole = new Role();
            adminRole.setName("ADMIN");
            adminRole.setDescription("Administrator with full access");
            
            // Add all permissions to admin
            List<Permission> allPermissions = permissionRepository.findAll();
            adminRole.getPermissions().addAll(allPermissions);
            
            roleRepository.save(adminRole);
            log.info("Created ADMIN role with {} permissions", allPermissions.size());
        }

        // Seller Role
        if (!roleRepository.existsByName("SELLER")) {
            Role sellerRole = new Role();
            sellerRole.setName("SELLER");
            sellerRole.setDescription("Seller with product and order management access");
            
            // Add seller permissions
            List<String> sellerPermissions = Arrays.asList(
                "PRODUCT_READ", "PRODUCT_WRITE", "PRODUCT_DELETE",
                "ORDER_READ", "ORDER_WRITE",
                "INVENTORY_READ", "INVENTORY_WRITE",
                "REPORT_READ"
            );
            
            sellerPermissions.forEach(permName -> {
                permissionRepository.findByName(permName).ifPresent(sellerRole::addPermission);
            });
            
            roleRepository.save(sellerRole);
            log.info("Created SELLER role");
        }

        // Customer Role
        if (!roleRepository.existsByName("CUSTOMER")) {
            Role customerRole = new Role();
            customerRole.setName("CUSTOMER");
            customerRole.setDescription("Customer with basic shopping access");
            
            // Add customer permissions
            List<String> customerPermissions = Arrays.asList(
                "PRODUCT_READ", "ORDER_READ", "ORDER_WRITE"
            );
            
            customerPermissions.forEach(permName -> {
                permissionRepository.findByName(permName).ifPresent(customerRole::addPermission);
            });
            
            roleRepository.save(customerRole);
            log.info("Created CUSTOMER role");
        }
    }

    private void createDefaultAdmin() {
        if (!userRepository.existsByEmail("admin@ecommerce.com")) {
            log.info("Creating default admin user...");
            
            User admin = new User();
            admin.setEmail("admin@ecommerce.com");
            admin.setFirstName("System");
            admin.setLastName("Administrator");
            admin.setPassword(passwordEncoder.encode("admin123!@#"));
            admin.setEmailVerified(true);
            admin.setStatus(User.UserStatus.ACTIVE);
            
            // Assign admin role
            Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();
            admin.addRole(adminRole);
            
            userRepository.save(admin);
            log.info("Default admin user created with email: admin@ecommerce.com");
        }
    }
}