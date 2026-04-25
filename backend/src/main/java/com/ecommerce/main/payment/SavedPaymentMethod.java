package com.ecommerce.main.payment;

import com.ecommerce.main.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "saved_payment_methods")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SavedPaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType type;

    /** Kullanıcının verdiği etiket: "İş Kartım", "PayPal Ana", "MetaMask ETH" */
    @Column(nullable = false)
    private String label;

    // ─── Stripe ───────────────────────────────
    /** Kart son 4 hanesi */
    private String cardLast4;
    /** Kart markası: Visa, Mastercard, … */
    private String cardBrand;
    /** Stripe PaymentMethod ID (test mode) */
    private String stripePaymentMethodId;

    // ─── PayPal ───────────────────────────────
    private String paypalEmail;

    // ─── Crypto ───────────────────────────────
    private String walletAddress;
    /** EVM chain ID: 1=Mainnet, 11155111=Sepolia, 137=Polygon, … */
    private Integer chainId;
    private String chainName;

    @Builder.Default
    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
