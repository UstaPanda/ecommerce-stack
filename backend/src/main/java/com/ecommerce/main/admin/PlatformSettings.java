package com.ecommerce.main.admin;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "platform_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformSettings {

    @Id
    private Long id = 1L;

    @Builder.Default
    private boolean maintenanceMode = false;

    @Builder.Default
    private boolean allowRegistrations = true;

    @Builder.Default
    private boolean requireEmailVerification = true;

    @Builder.Default
    private int maxOrdersPerUser = 50;

    @Builder.Default
    private String platformCurrency = "USD";

    @Builder.Default
    private String supportEmail = "support@zorlukurt.com";

    @Builder.Default
    private String platformName = "ZorluKurt Trading";

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
