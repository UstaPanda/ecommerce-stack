package com.ecommerce.main.shipment;

import com.ecommerce.main.order.Order;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "shipments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(name = "tracking_number", unique = true)
    private String trackingNumber;

    private String carrier;

    @Column(name = "warehouse_block")
    private String warehouseBlock;

    @Column(name = "mode_of_shipment")
    private String modeOfShipment;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private ShipmentStatus status = ShipmentStatus.PENDING;

    @Column(name = "estimated_delivery")
    private LocalDateTime estimatedDelivery;

    private Double latitude;
    private Double longitude;

    @Column(name = "last_location_update")
    private LocalDateTime lastLocationUpdate;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    // DS3 (E-Commerce Shipping Data)
    @Column(name = "customer_care_calls")
    private Integer customerCareCalls;

    @Column(name = "customer_rating")
    private Integer customerRating;

    @Column(name = "cost_of_product")
    private Double costOfProduct;

    @Column(name = "prior_purchases")
    private Integer priorPurchases;

    @Column(name = "discount_offered")
    private Double discountOffered;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
