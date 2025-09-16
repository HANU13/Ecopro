package com.litemax.ECoPro.repository.auth;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.litemax.ECoPro.entity.auth.Permission;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
		Optional<Permission> findByName(String name);
		boolean existsByName(String name);
}