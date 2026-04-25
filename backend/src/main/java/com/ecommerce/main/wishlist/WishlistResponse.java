package com.ecommerce.main.wishlist;

import java.time.LocalDateTime;

public record WishlistResponse(
        Long id,
        Long productId,
        String productName,
        Double unitPrice,
        String categoryName,
        String storeName,
        LocalDateTime addedAt
) {
    public static WishlistResponse from(WishlistItem w) {
        var p = w.getProduct();
        return new WishlistResponse(
                w.getId(),
                p.getId(),
                p.getName(),
                p.getUnitPrice(),
                p.getCategory() != null ? p.getCategory().getName() : null,
                p.getStore() != null ? p.getStore().getName() : null,
                w.getAddedAt()
        );
    }
}
