package com.ecommerce.main.store;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StoreRequest {
    @NotBlank
    private String name;
    private String description;
    private String address;
}
