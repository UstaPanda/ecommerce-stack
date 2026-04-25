package com.ecommerce.main.payment;

import java.time.LocalDateTime;

public record SavedPaymentMethodResponse(
        Long id,
        PaymentType type,
        String label,
        String cardLast4,
        String cardBrand,
        String stripePaymentMethodId,
        String paypalEmail,
        String walletAddress,
        Integer chainId,
        String chainName,
        boolean isDefault,
        LocalDateTime createdAt
) {
    public static SavedPaymentMethodResponse from(SavedPaymentMethod m) {
        return new SavedPaymentMethodResponse(
                m.getId(),
                m.getType(),
                m.getLabel(),
                m.getCardLast4(),
                m.getCardBrand(),
                m.getStripePaymentMethodId(),
                m.getPaypalEmail(),
                m.getWalletAddress(),
                m.getChainId(),
                m.getChainName(),
                m.isDefault(),
                m.getCreatedAt()
        );
    }
}
