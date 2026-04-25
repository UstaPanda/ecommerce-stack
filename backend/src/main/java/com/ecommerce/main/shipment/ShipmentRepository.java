package com.ecommerce.main.shipment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Optional<Shipment> findByOrderId(Long orderId);
    Optional<Shipment> findByTrackingNumber(String trackingNumber);
    Page<Shipment> findByStatus(ShipmentStatus status, Pageable pageable);
    Page<Shipment> findByOrderStoreId(Long storeId, Pageable pageable);
    Page<Shipment> findByOrderUserId(Long userId, Pageable pageable);
}
