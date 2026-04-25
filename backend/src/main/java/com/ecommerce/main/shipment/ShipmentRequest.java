package com.ecommerce.main.shipment;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ShipmentRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    private String carrier;
    private String warehouseBlock;
    private String modeOfShipment;
    private LocalDateTime estimatedDelivery;

    // DS3 (E-Commerce Shipping Data)
    private Integer customerCareCalls;
    private Integer customerRating;
    private Double costOfProduct;
    private Integer priorPurchases;
    private Double discountOffered;
}
