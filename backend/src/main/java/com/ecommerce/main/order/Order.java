package com.ecommerce.main.order;

import com.ecommerce.main.store.Store;
import com.ecommerce.main.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "grand_total", nullable = false)
    private Double grandTotal;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "shipping_address")
    private String shippingAddress;

    // DS4 (Amazon Sales): fulfilment channel, sales channel, shipping service level
    private String fulfilment;

    @Column(name = "sales_channel")
    private String salesChannel;

    @Column(name = "ship_service_level")
    private String shipServiceLevel;

    // DS5 (Pakistan E-Commerce): original order increment ID from source dataset
    @Column(name = "increment_id")
    private String incrementId;

    /** Blockchain transaction hash — only set for CRYPTO_WALLET payments */
    @Column(name = "tx_hash")
    private String txHash;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

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
