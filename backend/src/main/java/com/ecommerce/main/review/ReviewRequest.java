package com.ecommerce.main.review;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ReviewRequest {

    @NotNull
    private Long productId;

    @NotNull
    @Min(1) @Max(5)
    private Integer starRating;

    private String comment;
}
