package com.litemax.ECoPro.repository.order;

import com.litemax.ECoPro.entity.order.Shipment;
import com.litemax.ECoPro.entity.order.Shipment.ShipmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    
    Optional<Shipment> findByTrackingNumber(String trackingNumber);
    
    List<Shipment> findByOrderId(Long orderId);
    
    Page<Shipment> findByStatus(ShipmentStatus status, Pageable pageable);
    
    List<Shipment> findByStatusIn(List<ShipmentStatus> statuses);
}