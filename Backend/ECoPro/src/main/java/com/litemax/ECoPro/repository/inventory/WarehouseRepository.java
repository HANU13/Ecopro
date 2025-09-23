package com.litemax.ECoPro.repository.inventory;

import com.litemax.ECoPro.entity.inventory.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    Optional<Warehouse> findByName(String name);
    List<Warehouse> findByStatusTrue();
    List<Warehouse> findByCity(String city);
}