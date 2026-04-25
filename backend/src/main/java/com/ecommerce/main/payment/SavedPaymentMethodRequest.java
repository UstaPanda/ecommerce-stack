package com.ecommerce.main.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SavedPaymentMethodRequest {

    @NotNull
    private PaymentType type;

    @NotBlank
    @Size(max = 100)
    private String label;

    // ─── Stripe ───────────────────────────────
    private String cardLast4;
    private String cardBrand;
    private String stripePaymentMethodId;

    // ─── PayPal ───────────────────────────────
    private String paypalEmail;

    // ─── Crypto ───────────────────────────────
    private String walletAddress;
    private Integer chainId;
    private String chainName;

    private boolean isDefault;
}
