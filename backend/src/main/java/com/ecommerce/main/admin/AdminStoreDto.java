package com.ecommerce.main.admin;

import com.ecommerce.main.store.Store;

public record AdminStoreDto(
        Long id,
        String name,
        String description,
        String address,
        String status,
        String ownerName,
        String ownerEmail,
        String createdAt
) {
    public static AdminStoreDto from(Store s) {
        return new AdminStoreDto(
                s.getId(),
                s.getName(),
                s.getDescription(),
                s.getAddress(),
                s.getStatus() != null ? s.getStatus().name() : null,
                s.getOwner() != null ? s.getOwner().getName() : null,
                s.getOwner() != null ? s.getOwner().getEmail() : null,
                s.getCreatedAt() != null ? s.getCreatedAt().toString() : null
        );
    }
}
