package com.ecommerce.main.shipment;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;

    /** Corporate/Admin: gönderi oluşturur */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_CORPORATE', 'ROLE_ADMIN')")
    public ResponseEntity<ShipmentResponse> create(
            @Valid @RequestBody ShipmentRequest request,
            Authentication auth) {
        return ResponseEntity.ok(shipmentService.create(auth.getName(), request));
    }

    /** Tracking numarasıyla sorgula — herkese açık */
    @GetMapping("/track/{trackingNumber}")
    public ResponseEntity<ShipmentResponse> track(@PathVariable String trackingNumber) {
        return ResponseEntity.ok(shipmentService.getByTracking(trackingNumber));
    }

    /** Order'a ait gönderiyi getir */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<ShipmentResponse> getByOrder(
            @PathVariable Long orderId,
            Authentication auth) {
        return ResponseEntity.ok(shipmentService.getByOrderId(orderId, auth.getName()));
    }

    /** Individual: kendi gönderilerini listeler */
    @GetMapping("/my")
    @PreAuthorize("hasAuthority('ROLE_INDIVIDUAL')")
    public ResponseEntity<Page<ShipmentResponse>> getMyShipments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(shipmentService.getMyShipments(auth.getName(), pageable));
    }

    /** Corporate/Admin: store'a ait gönderileri listeler */
    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasAnyAuthority('ROLE_CORPORATE', 'ROLE_ADMIN')")
    public ResponseEntity<Page<ShipmentResponse>> getStoreShipments(
            @PathVariable Long storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(shipmentService.getStoreShipments(storeId, auth.getName(), pageable));
    }

    /** Corporate/Admin: konum günceller */
    @PatchMapping("/{id}/location")
    @PreAuthorize("hasAnyAuthority('ROLE_CORPORATE', 'ROLE_ADMIN')")
    public ResponseEntity<ShipmentResponse> updateLocation(
            @PathVariable Long id,
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            Authentication auth) {
        return ResponseEntity.ok(shipmentService.updateLocation(id, latitude, longitude, auth.getName()));
    }

    /** Corporate/Admin: gönderi durumunu günceller */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_CORPORATE', 'ROLE_ADMIN')")
    public ResponseEntity<ShipmentResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam ShipmentStatus status,
            Authentication auth) {
        return ResponseEntity.ok(shipmentService.updateStatus(id, status, auth.getName()));
    }
}
