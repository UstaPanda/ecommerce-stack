package com.ecommerce.main.coupon;

import java.math.BigDecimal;

public record CouponDto(
        Long id,
        String code,
        String discountType,
        BigDecimal discountValue,
        Long storeId,
        String storeName,
        Integer maxUses,
        Integer usedCount,
        Boolean active,
        String expiresAt,
        String createdAt
) {
    public static CouponDto from(Coupon c) {
        return new CouponDto(
                c.getId(),
                c.getCode(),
                c.getDiscountType(),
                c.getDiscountValue(),
                c.getStore() != null ? c.getStore().getId() : null,
                c.getStore() != null ? c.getStore().getName() : null,
                c.getMaxUses(),
                c.getUsedCount(),
                c.getActive(),
                c.getExpiresAt() != null ? c.getExpiresAt().toString() : null,
                c.getCreatedAt() != null ? c.getCreatedAt().toString() : null
        );
    }
}
