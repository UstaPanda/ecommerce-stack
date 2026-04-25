package com.ecommerce.main.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String sku;

    private String description;

    @NotNull
    @Min(0)
    private Double unitPrice;

    @Min(0)
    private Integer stockQuantity = 0;

    private Long categoryId;

    // DS3: "Low", "Medium", "High"
    private String productImportance;

    private String imageUrl;
}
