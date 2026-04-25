package com.ecommerce.main.address;

import java.time.LocalDateTime;

public record UserAddressResponse(
        Long id,
        String title,
        String fullAddress,
        String city,
        String district,
        String postalCode,
        boolean isDefault,
        LocalDateTime createdAt
) {
    public static UserAddressResponse from(UserAddress a) {
        return new UserAddressResponse(
                a.getId(), a.getTitle(), a.getFullAddress(),
                a.getCity(), a.getDistrict(), a.getPostalCode(),
                a.isDefault(), a.getCreatedAt()
        );
    }
}
