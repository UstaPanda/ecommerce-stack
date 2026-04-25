package com.ecommerce.main.customer;

import com.ecommerce.main.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "customer_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String gender;

    private Integer age;

    private String city;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "membership_type")
    private MembershipType membershipType = MembershipType.BRONZE;

    @Builder.Default
    @Column(name = "total_spend")
    private Double totalSpend = 0.0;

    @Builder.Default
    @Column(name = "items_purchased")
    private Integer itemsPurchased = 0;

    @Builder.Default
    @Column(name = "avg_rating")
    private Double avgRating = 0.0;

    @Builder.Default
    @Column(name = "discount_applied")
    private Boolean discountApplied = false;

    @Column(name = "satisfaction_level")
    private String satisfactionLevel;
}
