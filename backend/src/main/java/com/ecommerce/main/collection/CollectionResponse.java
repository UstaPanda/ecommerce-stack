package com.ecommerce.main.collection;

import java.time.LocalDateTime;
import java.util.List;

public record CollectionResponse(
        Long id,
        String name,
        String description,
        int itemCount,
        List<CollectionItemResponse> items,
        LocalDateTime createdAt
) {
    public record CollectionItemResponse(
            Long id,
            Long productId,
            String productName,
            Double unitPrice,
            String storeName,
            LocalDateTime addedAt
    ) {
        public static CollectionItemResponse from(CollectionItem ci) {
            var p = ci.getProduct();
            return new CollectionItemResponse(
                    ci.getId(),
                    p.getId(),
                    p.getName(),
                    p.getUnitPrice(),
                    p.getStore() != null ? p.getStore().getName() : null,
                    ci.getAddedAt()
            );
        }
    }

    public static CollectionResponse from(Collection c) {
        List<CollectionItemResponse> items = c.getItems().stream()
                .map(CollectionItemResponse::from).toList();
        return new CollectionResponse(
                c.getId(),
                c.getName(),
                c.getDescription(),
                items.size(),
                items,
                c.getCreatedAt()
        );
    }

    /** Lightweight variant — no item details, just count */
    public static CollectionResponse summary(Collection c) {
        return new CollectionResponse(
                c.getId(),
                c.getName(),
                c.getDescription(),
                c.getItems().size(),
                List.of(),
                c.getCreatedAt()
        );
    }
}
