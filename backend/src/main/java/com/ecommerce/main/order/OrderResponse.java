package com.ecommerce.main.order;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long userId,
        String userEmail,
        Long storeId,
        String storeName,
        OrderStatus status,
        Double grandTotal,
        Double subtotalSum,
        Double discountAmount,
        String paymentMethod,
        String shippingAddress,
        String fulfilment,
        String salesChannel,
        String shipServiceLevel,
        String incrementId,
        List<OrderItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public record OrderItemResponse(
            Long id,
            Long productId,
            String productName,
            String productSku,
            Integer quantity,
            Double unitPrice,
            Double subtotal
    ) {}

    public static OrderResponse from(Order order) {
        double subtotalSum = 0.0;
        List<OrderItemResponse> itemResponses = new java.util.ArrayList<>();
        
        for (OrderItem item : order.getItems()) {
            double itemSubtotal = item.getQuantity() * item.getUnitPrice();
            subtotalSum += itemSubtotal;
            itemResponses.add(new OrderItemResponse(
                    item.getId(),
                    item.getProduct().getId(),
                    item.getProduct().getName(),
                    item.getProduct().getSku(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    itemSubtotal
            ));
        }

        double discountAmount = Math.max(0, subtotalSum - order.getGrandTotal());

        return new OrderResponse(
                order.getId(),
                order.getUser().getId(),
                order.getUser().getEmail(),
                order.getStore().getId(),
                order.getStore().getName(),
                order.getStatus(),
                order.getGrandTotal(),
                subtotalSum,
                discountAmount,
                order.getPaymentMethod(),
                order.getShippingAddress(),
                order.getFulfilment(),
                order.getSalesChannel(),
                order.getShipServiceLevel(),
                order.getIncrementId(),
                itemResponses,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
