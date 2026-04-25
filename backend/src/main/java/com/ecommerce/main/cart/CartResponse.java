package com.ecommerce.main.cart;

import java.util.List;

public record CartResponse(
        Long cartId,
        Long storeId,
        String storeName,
        List<CartItemResponse> items,
        double totalPrice
) {
    public record CartItemResponse(
            Long cartItemId,
            Long productId,
            String productName,
            String productSku,
            double unitPrice,
            Integer quantity,
            double subtotal,
            String imageUrl
    ) {}

    public static CartResponse from(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(i -> new CartItemResponse(
                        i.getId(),
                        i.getProduct().getId(),
                        i.getProduct().getName(),
                        i.getProduct().getSku(),
                        i.getProduct().getUnitPrice(),
                        i.getQuantity(),
                        i.getProduct().getUnitPrice() * i.getQuantity(),
                        i.getProduct().getImageUrl()
                ))
                .toList();

        double total = itemResponses.stream().mapToDouble(CartItemResponse::subtotal).sum();

        Long storeId = cart.getItems().isEmpty() ? null
                : cart.getItems().get(0).getProduct().getStore().getId();
        String storeName = cart.getItems().isEmpty() ? null
                : cart.getItems().get(0).getProduct().getStore().getName();

        return new CartResponse(cart.getId(), storeId, storeName, itemResponses, total);
    }
}
