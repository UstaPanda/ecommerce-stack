package com.ecommerce.main.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OrderRequest {

    @NotNull(message = "Store ID is required")
    private Long storeId;

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderItemRequest> items;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;

    // DS4 (Amazon): optional fields
    private String fulfilment;
    private String salesChannel;
    private String shipServiceLevel;

    // DS5 (Pakistan): optional source increment ID
    private String incrementId;
}
