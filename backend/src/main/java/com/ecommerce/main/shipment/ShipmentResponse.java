package com.ecommerce.main.shipment;

import java.time.LocalDateTime;

public record ShipmentResponse(
        Long id,
        Long orderId,
        String trackingNumber,
        String carrier,
        String warehouseBlock,
        String modeOfShipment,
        ShipmentStatus status,
        LocalDateTime estimatedDelivery,
        LocalDateTime deliveredAt,
        Double latitude,
        Double longitude,
        LocalDateTime lastLocationUpdate,
        Integer customerCareCalls,
        Integer customerRating,
        Double costOfProduct,
        Integer priorPurchases,
        Double discountOffered,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ShipmentResponse from(Shipment s) {
        return new ShipmentResponse(
                s.getId(),
                s.getOrder().getId(),
                s.getTrackingNumber(),
                s.getCarrier(),
                s.getWarehouseBlock(),
                s.getModeOfShipment(),
                s.getStatus(),
                s.getEstimatedDelivery(),
                s.getDeliveredAt(),
                s.getLatitude(),
                s.getLongitude(),
                s.getLastLocationUpdate(),
                s.getCustomerCareCalls(),
                s.getCustomerRating(),
                s.getCostOfProduct(),
                s.getPriorPurchases(),
                s.getDiscountOffered(),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }
}
